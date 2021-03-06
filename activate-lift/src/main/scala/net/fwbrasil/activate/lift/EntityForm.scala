package net.fwbrasil.activate.lift

import net.fwbrasil.activate.entity.map.EntityMap
import net.fwbrasil.activate.entity.BaseEntity
import net.fwbrasil.activate.ActivateContext
import net.liftweb.util.FieldError
import net.fwbrasil.activate.entity.InvariantViolationException
import net.liftweb.http.S
import net.liftweb.util.FieldIdentifier
import net.liftweb.common.Box
import net.fwbrasil.activate.entity.map.EntityMapBase

class EntityForm[E <: BaseEntity] private[activate] (values: Map[String, Any])(implicit m: Manifest[E], context: ActivateContext) extends EntityMap[E](values) {

    def this(entity: E)(implicit m: Manifest[E], context: ActivateContext) =
        this(entity.vars.map(ref => (ref.name, EntityMapBase.varToValue(ref))).toMap)

    def this(init: ((E) => (_, _))*)(implicit m: Manifest[E], context: ActivateContext) =
        this(init.map(EntityMapBase.keyAndValueFor[E](_)(m)).toMap)

    override def createEntity =
        EntityForm.translateInvariantsExceptions {
            super.createEntity
        }

    override def updateEntity(entity: E, values: Map[String, Any]) =
        EntityForm.translateInvariantsExceptions {
            super.updateEntity(entity, values)
        }

}

object EntityForm {
    protected[activate] def translateInvariantsExceptions[R](f: => R) =
        try f
        catch {
            case ex: InvariantViolationException =>
                val errors =
                    for (violation <- ex.violations.toList) yield {
                        if (violation.properties.isEmpty)
                            throw ex
                        val message = S.?(violation.invariantName)
                        for (property <- violation.properties) yield FieldError(
                            new FieldIdentifier {
                                override def uniqueFieldId = Box.legacyNullTest(property)
                            }, message)
                    }
                throw InvalidForm(errors.flatten.toList)
        }
}

case class InvalidForm(errors: List[FieldError]) extends Exception
