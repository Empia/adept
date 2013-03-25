package adept.core.parsers

import adept.core.models._

object Parsers {
  val CoordsExpr = """\s*(.*):(.*):(.*)\s*""".r
  val CoordsMetadataExpr = """\s*(.*?)(\[(.*)\])?\s*""".r
  val MetadataBracketsExpr = """\s*\[(.*)\]\s*""".r
  val MetadataExpr = """\s*(.*)=(.*)\s*""".r
  val CoordsMetadataHashExpr = """\s*(.*)@(.*)\s*""".r
  
  def shortModule(string: String): Either[String, (Coordinates, Metadata, Hash)] = {
    string match {
      case CoordsMetadataHashExpr(coordsMeta, hash) => {
        coordsMetadata(coordsMeta).right.map{ case (coords, meta) =>
          (coords, meta, Hash(hash))
        }
      }
      case noHash => Left(s"could not find required hash in $noHash")
    }
  }
  
  def coords(string: String): Either[String, Coordinates] = string match {
    case CoordsExpr(org, name, version) => {
      if (org.contains(" ") || name.contains(" ") || version.contains(" "))
        Left(s"could not parse coordinates because of whitespace in expression: '$string'")
      else
        Right(Coordinates(org, name, version))
    }
    case noCoords => Left(s"could not parse coordinates: $noCoords")
  }
  
  def metadata(string: String): Either[String, Metadata] = {
    string match {
      case MetadataBracketsExpr(pairs) => {
        val foundMetaEntries = pairs.split(",").map{ entry =>
          entry match {
            case "" => Right(None)
            case MetadataExpr(key, value) => Right(Some(key -> value))
            case noMeta => Left(s"could not parse metadata $string because of syntax error around: '$noMeta'")
          }
        }
        foundMetaEntries.foldLeft[Either[String, Metadata]](Right(Metadata(Map.empty))){ (product, current) =>
          for {
            p <- product.right
            perhapsC <- current.right
          } yield {
            perhapsC.map { c => 
              Metadata(p.data + c)
            }.getOrElse {
              Metadata(p.data)
            }
          }
        }
      }
      case noBrackets => Left(s"expected metadata to be closed by [ and ], instead found: $noBrackets") 
    }
  }
  
  def coordsMetadata(string: String): Either[String, (Coordinates, Metadata)] = string match {
    case CoordsMetadataExpr(coordsString, metadataString, _) =>
      val foundCoords = Parsers.coords(coordsString)
      val foundMetadata = (for {
        metadata <- Option(metadataString)
      } yield {
        Parsers.metadata(metadata)
      }).getOrElse{ Right(Metadata(Map.empty)) }
      for {
        coords <- foundCoords.right
        metadata <- foundMetadata.right
      } yield {
        coords -> metadata
      }
    case somethingElse => Left(s"could not parse coordinates in: $somethingElse")
  } 
  
  def setToString(set: Set[_]) = set.mkString("[",",","]")
  def setFromString(s: String) = {
    val Expr = """\[(.*?)\]""".r
    s match {
      case Expr(list) => Right(list.split(",").toSet.filter(_.trim.nonEmpty))
      case something => Left(s"could not parse sequence: $s")
    }
  }
}