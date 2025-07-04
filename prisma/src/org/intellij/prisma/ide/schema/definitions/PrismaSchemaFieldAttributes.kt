package org.intellij.prisma.ide.schema.definitions

import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import org.intellij.prisma.ide.completion.PrismaInsertHandler
import org.intellij.prisma.ide.schema.PrismaSchemaKind
import org.intellij.prisma.ide.schema.builder.schema
import org.intellij.prisma.ide.schema.types.PrismaDatasourceProviderType
import org.intellij.prisma.ide.schema.types.PrismaDatasourceProviderType.*
import org.intellij.prisma.lang.PrismaConstants.FieldAttributes
import org.intellij.prisma.lang.PrismaConstants.Functions
import org.intellij.prisma.lang.PrismaConstants.ParameterNames
import org.intellij.prisma.lang.PrismaConstants.Types
import org.intellij.prisma.lang.psi.PrismaModelDeclaration
import org.intellij.prisma.lang.psi.PrismaPsiPatterns
import org.intellij.prisma.lang.psi.PrismaTableEntityDeclaration
import org.intellij.prisma.lang.types.PrismaBooleanType
import org.intellij.prisma.lang.types.PrismaPrimitiveType
import java.util.*

val PRISMA_SCHEMA_FIELD_ATTRIBUTES = schema {
  group(PrismaSchemaKind.FIELD_ATTRIBUTE) {
    element {
      label = FieldAttributes.ID
      documentation = "Defines a single-field ID on the model."
      pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java))

      param {
        label = ParameterNames.MAP
        insertHandler = PrismaInsertHandler.COLON_QUOTED_ARGUMENT
        documentation = "Defines a custom primary key name in the database."
        type = "String?"
      }
      length()
      sort(datasourceTypes = EnumSet.of(SQLSERVER))
      clustered()
    }

    element {
      label = FieldAttributes.UNIQUE
      documentation = "Defines a unique constraint for this field."
      pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java))

      param {
        label = ParameterNames.MAP
        insertHandler = PrismaInsertHandler.COLON_QUOTED_ARGUMENT
        documentation = "Defines a custom name for the unique constraint in the database."
        type = "String?"
      }
      length()
      sort()
      clustered()
    }

    element {
      label = FieldAttributes.MAP
      insertHandler = PrismaInsertHandler.PARENS_QUOTED_ARGUMENT
      documentation = "Maps a field name from the Prisma schema to a different column name."

      param {
        label = ParameterNames.NAME
        documentation = "The name of the target database column."
        type = "String"
      }
    }

    element {
      label = FieldAttributes.DEFAULT
      insertHandler = ParenthesesInsertHandler.WITH_PARAMETERS
      documentation = "Defines a default value for this field. `@default` takes an expression as an argument."
      pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaTableEntityDeclaration::class.java))

      param {
        label = ParameterNames.EXPRESSION
        documentation = "An expression (e.g. `5`, `true`, `now()`)."
        type = "Expression"
        skipInCompletion = true

        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.DBGENERATED
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.AUTO
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.SEQUENCE
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.AUTOINCREMENT
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.NOW
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.CUID
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.UUID
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.ULID
          }
        }
        variant {
          ref {
            kind = PrismaSchemaKind.FUNCTION
            label = Functions.NANOID
          }
        }

        booleanTypeValues(PrismaPsiPatterns.withFieldType(true) { type, _ -> type is PrismaBooleanType })
      }
    }

    element {
      label = FieldAttributes.RELATION
      insertHandler = ParenthesesInsertHandler.WITH_PARAMETERS
      documentation =
        "Defines a connection between two models. [Learn more](https://pris.ly/d/relation-attribute)."
      pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java))

      param {
        label = ParameterNames.NAME
        insertHandler = PrismaInsertHandler.COLON_QUOTED_ARGUMENT
        documentation =
          "Defines the name of the relationship. In an m-n-relation, it also determines the name of the underlying relation table."
        type = "String?"
      }
      param {
        label = ParameterNames.MAP
        insertHandler = PrismaInsertHandler.COLON_QUOTED_ARGUMENT
        documentation = "Defines a custom name for the foreign key in the database."
        type = "String?"
        datasources = PrismaDatasourceProviderType.except(MONGODB)
      }
      param {
        label = ParameterNames.FIELDS
        insertHandler = PrismaInsertHandler.COLON_LIST_ARGUMENT
        documentation = "A list of field references of the *current* model."
        type = "FieldReference[]?"
      }
      param {
        label = ParameterNames.REFERENCES
        insertHandler = PrismaInsertHandler.COLON_LIST_ARGUMENT
        documentation = "A list of field references of the model on *the other side of the relation*."
        type = "FieldReference[]?"
      }
      param {
        label = ParameterNames.ON_DELETE
        documentation =
          "Specifies the action to perform when a referenced entry in the referenced model is being deleted. [Learn more](https://pris.ly/d/referential-actions)."
        type = Types.REFERENTIAL_ACTION.optional()
        datasources = PrismaDatasourceProviderType.except(MONGODB)

        variantsForType(Types.REFERENTIAL_ACTION)
      }
      param {
        label = ParameterNames.ON_UPDATE
        documentation =
          "Specifies the action to perform when a referenced field in the referenced model is being updated to a new value. [Learn more](https://pris.ly/d/referential-actions)."
        type = Types.REFERENTIAL_ACTION.optional()
        datasources = PrismaDatasourceProviderType.except(MONGODB)

        variantsForType(Types.REFERENTIAL_ACTION)
      }
    }

    element {
      label = FieldAttributes.UPDATED_AT
      documentation = "Automatically stores the time when a record was last updated."
      pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java))
    }

    element {
      label = FieldAttributes.IGNORE
      documentation =
        "A field with an `@ignore` attribute can be kept in sync with the database schema using Prisma Migrate and Introspection, but won't be exposed in Prisma Client."
      pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java))
    }

    element {
      label = FieldAttributes.SHARD_KEY
      documentation = "The `@shardKey` attribute is only compatible with [PlanetScale](https://planetscale.com/) databases. It enables you to define a [shard key](https://planetscale.com/docs/vitess/sharding) on multiple fields of your model."
      pattern = StandardPatterns.and(
        PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java)),
        PrismaPsiPatterns.withFieldType { type, _ -> type is PrismaPrimitiveType },
      )
      datasources = EnumSet.of(MYSQL)
    }
  }

  deferred(PrismaSchemaKind.FIELD_ATTRIBUTE) { ctx ->
    ctx.metadata?.datasources?.forEach { (name) ->
      element {
        label = "@$name"
        documentation =
          "Defines a native database type that should be used for this field. See https://www.prisma.io/docs/concepts/components/prisma-schema/data-model#native-types-mapping."
        insertHandler = PrismaInsertHandler.QUALIFIED_NAME
        datasources = PrismaDatasourceProviderType.except(SQLITE)
        pattern = PrismaPsiPatterns.insideEntityDeclaration(psiElement(PrismaModelDeclaration::class.java))
      }
    }
  }
}