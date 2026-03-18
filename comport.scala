//> using platform native
//> using scala 3.3
//> using options -no-indent -rewrite
//> using dep com.lihaoyi::mainargs::0.7.8
//> using dep com.lihaoyi::os-lib::0.11.8
//> using dep org.virtuslab::scala-yaml::0.3.1

import mainargs.{main, arg, Flag, ParserForMethods}
import org.virtuslab.yaml.*
import scala.scalanative.unsafe._

// POSIX _exit(2): terminates the process immediately without running
// atexit handlers or GC finalization.  This avoids a race condition
// between the GC cleanup on the main thread and os-lib's
// GenericProcessWatcher daemon thread, which causes intermittent
// segfaults on musl-linked static binaries.
@extern
private object PosixExit {
  @name("_exit")
  def posixExit(status: CInt): Unit = extern
}

// ---- Parse compose.yaml ----

// We only need the top-level keys under services:, so Map[String, Any] suffices
case class ComposeFile(services: Map[String, Any]) derives YamlDecoder

/** Look for compose.yaml / docker-compose.yaml in the worktree root and return service names */
def detectServices(root: os.Path): Either[String, List[String]] = {
  val candidates = List(
    "compose.yaml",
    "compose.yml",
    "docker-compose.yaml",
    "docker-compose.yml"
  )
  candidates
    .map(root / _)
    .find(os.exists)
    .toRight("compose file not found")
    .flatMap { path =>
      os.read(path).as[ComposeFile] match {
        case Right(cf) if cf.services.nonEmpty =>
          Right(cf.services.keys.toList.sorted)
        case Right(_)  => Left("services section is empty")
        case Left(err) => Left(s"YAML parse error: $err")
      }
    }
}

// ---- Hash: FNV-1a 64bit ----

def fnv1a64(s: String): Long = {
  var hash = 0xcbf29ce484222325L
  for c <- s do hash ^= c.toLong
  hash *= 0x100000001b3L
  hash
}

// ---- Port allocation logic ----

case class Allocation(
    worktreePath: String,
    basePort: Int,
    ports: List[(String, Int)],
    slotCount: Int,
    slot: Int
)

def allocate(
    path: String,
    names: List[String],
    base: Int,
    range: Int
): Allocation = {
  val numServices = names.length
  val hash = fnv1a64(path)
  val slotCount = range / numServices
  val slot = math.abs(hash % slotCount).toInt
  val basePort = base + slot * numServices
  val ports =
    names.zipWithIndex.map((name, i) => (name.toUpperCase, basePort + i))
  Allocation(path, basePort, ports, slotCount, slot)
}

// ---- Entry point ----

object Comport {
  @main
  def comport(
      @arg(doc = "Number of services (used when --names is not specified and no compose file is found)")
      numServices: Int = 1,
      @arg(doc = "Service names (comma-separated). Overrides compose file auto-detection when specified")
      names: String = "",
      @arg(doc = "Environment variable prefix")
      prefix: String = "PORT",
      @arg(doc = "Start of the port range")
      base: Int = 10000,
      @arg(doc = "Width of the port range")
      range: Int = 50000,
      @arg(doc = "Show allocation info on stderr")
      show: Flag = Flag(),
      @arg(doc = "Output in .env format without export")
      dotenv: Flag = Flag()
  ): Unit = {
    val worktreePath =
      try os.proc("git", "rev-parse", "--show-toplevel").call().out.trim()
      catch {
        case _ => {
          System.err.println("Error: not in a git repository")
          sys.exit(1)
        }
      }

    // Resolve service names: --names > compose auto-detection > numServices
    val resolvedNames: List[String] =
      if names.nonEmpty then
      // Explicit specification takes highest priority
      names.split(",").toList.map(_.trim).filter(_.nonEmpty)
      else
        detectServices(os.Path(worktreePath)) match {
          case Right(detected) => {
            if show.value then System.err.println(
              s"[info] detected services from compose file: ${detected.mkString(", ")}"
            )
            detected
          }
          case Left(reason) => {
            if show.value then System.err.println(
              s"[info] compose file not used ($reason), falling back to numServices=$numServices"
            )
            (0 until numServices).map(_.toString).toList
          }
        }

    val alloc = allocate(worktreePath, resolvedNames, base, range)

    val pfx = if dotenv.value then "" else "export "
    println(s"${pfx}${prefix}_BASE=${alloc.basePort}")
    alloc.ports.foreach { (name, port) =>
      println(s"${pfx}${prefix}_${name}=${port}")
    }
    if show.value then System.err.println(
      s"""|--- worktree-ports ---
            |worktree  : ${alloc.worktreePath}
            |hash      : ${fnv1a64(alloc.worktreePath)}
            |slot      : ${alloc.slot} / ${alloc.slotCount}
            |base port : ${alloc.basePort}
            |services  : ${resolvedNames.length}
            |range     : ${alloc.basePort} - ${alloc.basePort + resolvedNames.length - 1}
            |----------------------""".stripMargin
    )
  }

  def main(args: Array[String]): Unit = {
    ParserForMethods(this).runOrExit(args.toSeq)
    // Skip GC finalization to avoid segfault on musl (see PosixExit above)
    PosixExit.posixExit(0)
  }
}
