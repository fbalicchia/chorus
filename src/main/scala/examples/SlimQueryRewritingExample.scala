package examples

import chorus.analysis.histogram.HistogramAnalysis
import chorus.rewriting.differential_privacy.{ElasticSensitivityConfig, ElasticSensitivityRewriter, SampleAndAggregateConfig, SampleAndAggregateRewriter}
import chorus.schema.Schema
import chorus.sql.QueryParser
import chorus.util.ElasticSensitivity

/** A simple example demonstrating query rewriting for differential privacy.
  */
object SlimQueryRewritingExample extends App {
  // Use the table schemas and metadata defined by the test classes
  System.setProperty("schema.config.path", "src/test/resources/schema.yaml")
  val database = Schema.getDatabase("test")

  // privacy budget
  val EPSILON = 0.1
  // delta parameter: use 1/n^2, with n = 100000
  val DELTA = 1 / (math.pow(100000,2))

  // Helper function to print queries with indentation.
  def printQuery(query: String) = println(s"\n  " + query.replaceAll("\\n", s"\n  ") + "\n")

  def elasticSensitivityExample() = {
    println("*** Elastic sensitivity example ***")

    // Example query: How many US customers ordered product #1?
    val query = """
      |SELECT COUNT(*) FROM orders
      |JOIN customers ON orders.customer_id = customers.customer_id
      |WHERE orders.product_id = 1 AND customers.address LIKE '%United States%'"""
      .stripMargin.stripPrefix("\n")

    // Print the example query and privacy budget
    val root = QueryParser.parseToRelTree(query, database)
    println("Original query:")
    printQuery(query)
    println(s"> Epsilon: $EPSILON")

    // Compute mechanism parameter values from the query. Note the rewriter does this automatically; here we calculate
    // the values manually so we can print them.
    val elasticSensitivity = ElasticSensitivity.smoothElasticSensitivity(root, database, 0, EPSILON, DELTA)
    println(s"> Elastic sensitivity of this query: $elasticSensitivity")
    println(s"> Required scale of Laplace noise: 2 * $elasticSensitivity / $EPSILON = ${2 * elasticSensitivity/EPSILON}")

    // Rewrite the original query to enforce differential privacy using Elastic Sensitivity.
    println("\nRewritten query:")
    val config = new ElasticSensitivityConfig(EPSILON, DELTA, database)
    val rewrittenQuery = new ElasticSensitivityRewriter(config).run(query)
    printQuery(rewrittenQuery.toSql())
  }
  elasticSensitivityExample()
}
