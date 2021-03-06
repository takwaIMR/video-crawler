package emails

import java.io._
import java.util.Date

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrDocument
import org.json.JSONObject
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.ops.transforms.Transforms
import util.{NLP, Semantic}

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport

object ConceptSearchEmails {
  def first(document: JSONObject, strings: Seq[String]): Option[String] = {
    strings.filter(
      (key) => document.has(key) && (
        Option(document.get(key)) match {
          case Some("") => false
          case None => false
          case _ => true
        }
        )
    ).headOption map {
      document.get(_).toString
    }
  }

  def normalizeUrl(originalUrl: String): String = {
    val result = originalUrl.split("[?]")(0)
    val url =
      if (result.endsWith("/")) {
        result.substring(0, result.length - 1)
      } else {
        result
      }

    url + "?utm_source=findlectures"
  }

  val modelFile = "D:\\projects\\clones\\pathToSaveModel10_10_1000_5_1510799977189.txt"
  val w2v = new Semantic(modelFile)
  w2v.init

  // todo can this happen while other stuff is going on?
  val model = w2v.model.getOrElse(???)

  var getWordVectorsMeanCache = Map[List[String], INDArray]()
  def getWordVectorsMean(tokens: List[String]): INDArray = {
    val key =
      tokens.filter(
        model.getWordVector(_) != null
      )

    if (!getWordVectorsMeanCache.contains(key)) {
      val output: INDArray = model.getWordVectorsMean(key.asJavaCollection)
      getWordVectorsMeanCache = getWordVectorsMeanCache + (key -> output)
    }

    // TODO: database-ize
    getWordVectorsMeanCache(key)
  }

  def main(args: Array[String]): Unit = {
    val query = args(0)
    val dataType = new VideoDataType

    // TODO port unit tests?
    // TODO get data from Google spreadsheet
    // TODO how long does this take if you do 200 emails instead of one
    // TODO templates for Aweber emails

    val textTemplate = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/alerts.txt")).mkString
    val htmlTemplate = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/alerts.html")).mkString

    println(textTemplate)
    println(htmlTemplate)

    println(new Date)
    val links1 = generate(query, new VideoDataType)
    println(new Date)

    println(new Date)
    val links2 = generate(query, new ArticleDataType)
    println(new Date)

  }

  def generate(query: String, dataType: DataType) {
    // TODO cluster by nearness? -> problems here:
    //    distance metric is an angle
    //    distance metric in N dimensions so be careful
    //
    query.split(",").map(
      (term: String) =>
        (term, getWordVectorsMean(term.split(" ").toList))
    )

    val queryWords = NLP.getWords(query)

    // STEPS:
    //   stay running, take queries (server?)
    //   get user's query
    //      split into related & unrelated
    //   get usere's things to remove
    //   POST to Solr
    //   get [talks, articles] from Solr (many)
    //      can this be Rocchio?
    //   re-sort by 'aboutness', top N
    //   re-sort by diversity
    //  get reddit updater to work against the same core as new stuff
    def listDocuments(dt: DataType, qq: String, rows: Integer, skip: List[String]): List[SolrDocument] = {
      import scala.collection.JavaConversions._

      val solrUrl = "http://40.87.64.225:8983/solr/" + dt.core

      val solr = new HttpSolrClient(solrUrl)

      val query = new SolrQuery()

      // todo remove negative terms

      val userQuery = qq
      query.setQuery( qq )
      query.setFields((dt.fieldsToRetrieve ++ dt.textFields).toArray: _*)
      query.setRequestHandler("tvrh")
      query.setRows(rows)
      skip.map(
        (id) => {
          query.addFilterQuery("-id:" + id)
        }
      )

      dt.filter match {
        case Some(value: String) => query.addFilterQuery(value)
        case None => {}
      }

      val rsp = solr.query( query )

      val result = rsp.getResults().toList

      result
    }

    case class Link(title: String, url: String, text: String, score: Float)

    val rowsToPull = 100

    val documentsSolr =
      listDocuments(
          dataType,
          query.split(",").map(
            (token) => (
              // TODO: ANDs vs ORs
              dataType.fieldsToQuery.map(
                (f) => f._1 + "\"" + token + "\"^" + f._2
              )
            )
          ).toList.mkString(" OR "),
          rowsToPull,
          List("1", "2", "3")
        ).filter(
          _ != null
        ).filter(
          dataType.postFilter
        ).map(
          (doc) =>
            Link(
              doc.get(dataType.titleField).toString,
              normalizeUrl(dataType.urlField(doc)),
              dataType.textFields.map(
                (field) => doc.get(field)
              ).mkString("\n"),
              doc.get("score").asInstanceOf[Float]
            )
      ).groupBy(_.url).map(
        // TODO: get shortest title
        (grp) => grp._2(0)
      ).toList.par

    val startTime = new Date
    println(startTime)

    // TODO : caching - in this case each query would potentially duplicate
    val queryMean = getWordVectorsMean(queryWords)

    val threads = 16
    documentsSolr.tasksupport = new ForkJoinTaskSupport(
      new scala.concurrent.forkjoin.ForkJoinPool(threads))

    val mostAbout =
      documentsSolr.map(
        (document: Link) =>
          (
            NLP.getWords(document.text),
            document
          )
      ).map(
        (document) => {
          val mean = getWordVectorsMean(document._1)

          (document._1, document._2, mean)
        }
      ).map(
        (vec) => (vec._2, vec._1, Transforms.cosineSim(vec._3, queryMean))
      ).toList // removes par
        .sortBy(
          (vec) => vec._3
        ).reverse

    //mostAbout.tasksupport = new ForkJoinTaskSupport(
    //  new scala.concurrent.forkjoin.ForkJoinPool(threads))

    def pickNext(
                  topDocuments: List[(Link, List[String], INDArray)],
                  remaining: List[(Link, List[String], INDArray)]
                ): (Double, (Link, List[String], INDArray)) = {
      val next =
        remaining.par.map(
          (tuple) => {
            val chosenMean =
              topDocuments.map(
                (doc) => doc._3
              ).reduce(
                (a, b) => a.add(b)
              ).div(topDocuments.length)

            // compare this document to the stuff we already chose
            // this technique will bring back things the most unlike the rest of the collection

            // could also compute this score and skip things that are two close
            (Transforms.cosineSim(chosenMean, tuple._3), tuple)
          }
        ).toList.sortBy(_._1)

      next.head
    }

    def recurse(
                 idx: Integer,
                 topDocuments: List[(Double, (Link, List[String], INDArray))],
                 remaining: List[(Link, List[String], INDArray)]
               ): List[(Double, (Link, List[String], INDArray))] = {
      val nextDocument = pickNext(topDocuments.map((vec) => vec._2), remaining)

      if (idx == 1) {
        List(nextDocument)
      } else {
        val nextRemaining = remaining.filter(
          (doc) => doc._1 != nextDocument._2._1
        )

        val nextTopDocumentList = nextDocument :: topDocuments
        nextDocument :: recurse(idx - 1, nextTopDocumentList, nextRemaining)
      }
    }

    val mostAboutMeans =
      mostAbout.map(
        (tuple) => (
          tuple._1,
          tuple._2,
          getWordVectorsMean(tuple._2)
        )
      )

    val diverse =
      (1.0, mostAboutMeans.head) :: recurse(
        10,
        List((1.0, mostAboutMeans.head)),
        mostAboutMeans.tail
      )

    diverse.map(
      (doc) => (doc._2)
    ).take(
      10
    ).map(
      (doc) => (doc._1.title, doc._1.url)
    )
  }
}