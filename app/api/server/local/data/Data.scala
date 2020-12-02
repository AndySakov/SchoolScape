package api.server.local.data

import java.nio.file.{Files, NoSuchFileException, Paths, StandardOpenOption}
import java.util.NoSuchElementException

import com.sun.net.httpserver.Authenticator.Success

import scala.util.Failure

object Data {

  /**
    * MorseCode code definition
    */
  object Morse {
    /**
      * Mapping of letters of the english alphabet to their <code>encoder.language.MorseCode</code> code equivalents
      */
    private val dict: Map[String, String] = Map(
      "a" -> "._", "A" -> "*._", "1" -> ".----",
      "b" -> "_...", "B" -> "*_...", "2" -> "..---",
      "c" -> "_._.", "C" -> "*_._.", "3" -> "...--",
      "d" -> "_..", "D" -> "*_..", "4" -> "....-",
      "e" -> ".", "E" -> "*.", "5" -> ".....",
      "f" -> ".._.", "F" -> "*.._.", "6" -> "-....",
      "g" -> "__.", "G" -> "*__.", "7" -> "--...",
      "h" -> "....", "H" -> "*....", "8" -> "---..",
      "i" -> "..", "I" -> "*..", "9" -> "----.",
      "j" -> ".___", "J" -> "*.___", "0" -> "-----",
      "k" -> "_._", "K" -> "*_._",
      "l" -> "._..", "L" -> "*._..",
      "m" -> "__", "M" -> "*__",
      "n" -> "_.", "N" -> "*_.",
      "o" -> "___", "O" -> "*___",
      "p" -> ".__.", "P" -> "*.__.",
      "q" -> "__._", "Q" -> "*__._",
      "r" -> "._.", "R" -> "*._.",
      "s" -> "...", "S" -> "*...",
      "t" -> "_", "T" -> "*_",
      "u" -> ".._", "U" -> "*.._",
      "v" -> "..._", "V" -> "*..._",
      "w" -> ".__", "W" -> "*.__",
      "x" -> "_.._", "X" -> "*_.._",
      "y" -> "_.__", "Y" -> "*_.__",
      "z" -> "__..",  "Z" -> "*__.."
    )
    def isWord(string: String): Boolean = string.split("").length > 1
    def isLetter(string: String): Boolean = string.split("").length == 1

    /**
      * encode(data: String): String = one letter at a time conversion of the letters
      *
      * @param data the english data to <code>convert</code> to <code>encoder.language.MorseCode</code> code
      * @return String representation of the <code>encoder.language.MorseCode</code> code for the param
      */
    def encode(data: String): String = {
      def wordEncode(s: String): String = s.split("").map(letterEncode).mkString("(", "|", ")")

      def letterEncode(s: String): String = {
        s match {
          case "." => "?"
          case "_" => "~"
          case " " => ";"
          case "\t" => "\t"
          case _ =>
            s"${dict.getOrElse(s, s)}"
        }
      }
      def sLetterEncode(s: String): String = {
        s match {
          case "." => "?"
          case "_" => "~"
          case " " => ";"
          case "\t" => "\t"
          case _ =>
            s"${dict.getOrElse(s, s)}"
        }
      }
      data match {
        case isWord(data) => wordEncode(data)
        case isLetter(data) => sLetterEncode(data)
        case _ => s"$data"
      }
    }

    /**
      * decode(data: String): String = one code code at a time conversion of the codes
      * @param data the encoder.language.MorseCode code to convert to english letters
      * @return String representation of the english form of the param
      */
    def decode(data: String): String = {
      def letterDecode(m: String): String = {
        m match {
          case "?" => "."
          case "~" => "_"
          case ";" => " "
          case "\t" => "\t"
          case _ =>
            val all = m.split("")
            val p = all.filterNot(x => x.equals("(")).filterNot(y => y.equals(")")).mkString
            Try(dict.filter(x => x._2 == p).head._1) match {
              case Success(value) =>
                value
              case Failure(exception) => exception match {
                case _: NoSuchElementException =>
                  println(s"Morse code $m has no english equivalent")
                  m
              }
            }

        }
      }
      def pLetterDecode(m: String): String ={
        case "?" => "."
        case "~" => "_"
        case ";" => " "
        case "\t" => "\t"
        case _ =>
          Try(dict.filter(x => x._2 == m).head._1) match {
            case Success(value) =>
              value
            case Failure(exception) =>
              exception match {
                case _:NoSuchElementException =>
                  println(s"Morse code $m has no english equivalent")
                  m
              }
          }
      }
      def wordDecode(m: String): String = {
        val all = m.split("")
        val letters = all.flatMap(x => if(x.equals("|")) " " else x)
        val ty = letters.mkString.split(" ")
        val brokenHead = ty.head.split("")
        val noBrh = brokenHead.filterNot(x => x.equals(brokenHead.head))
        val head = Array(noBrh.mkString)
        val brokenTail = ty.last.split("")
        val noBrt = brokenTail.filterNot(x => x.equals(brokenTail.last))
        val tail = Array(noBrt.mkString)
        val mid = ty.filterNot(x => x.equals(ty.head)).filterNot(p => p.equals(ty.last))
        val end = head ++ mid ++ tail
        val de = end.flatMap(pLetterDecode).mkString
        de
      }
      data match {
        case isLetter(data) => letterDecode(data)
        case isWord(data) => wordDecode(data)
        case _ => "?"
      }
    }
  }

  private val HOME_DIRECTORY: String = sys.env("USERPROFILE")
  private val DATA_SOURCE: String = s"$HOME_DIRECTORY/.schoolscape/data"
  private val NAME_STORE: String = s"$DATA_SOURCE/student/stored-bundles.hex"
  var mode: StartMode = StartMode.OFFLINE

  def saveName(name: String): Unit = Files.write(Paths.get(NAME_STORE), Morse.encode(name).getBytes, StandardOpenOption.APPEND)

  def user: String = {
    Try(Files.readAllLines(Paths.get(NAME_STORE))
      .toArray.map(x => {
      Morse.decode(x.toString)
    }).head) match {
      case Success(value) =>
        value
      case Failure(exception) =>
        exception match {
          case _: NoSuchFileException =>
            Files.createFile(Paths.get(NAME_STORE))
            "N/A"
          case _: NoSuchElementException =>
            "N/A"
        }
    }
  }
  def flushName(): Unit = Files.write(Paths.get(NAME_STORE), "".getBytes, StandardOpenOption.TRUNCATE_EXISTING)
  def setMode(mode: StartMode): Unit = this.mode = mode
}
