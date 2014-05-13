package uk.co.scassandra.priming.prepared

import uk.co.scassandra.cqlmessages.ColumnType
import uk.co.scassandra.priming.query.Prime

case class PreparedPrime(
                  variableTypes: List[ColumnType[_]] = List(),
                  prime: Prime = Prime()
                  )