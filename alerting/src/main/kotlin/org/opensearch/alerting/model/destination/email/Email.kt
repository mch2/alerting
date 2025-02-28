/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.model.destination.email

import org.opensearch.alerting.util.isValidEmail
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.XContentBuilder
import org.opensearch.common.xcontent.XContentParser
import org.opensearch.common.xcontent.XContentParser.Token
import org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken
import java.io.IOException
import java.lang.IllegalStateException
import java.util.Locale

/**
 * A value object that represents an Email message. Email messages will be
 * submitted to the Email destination.
 */
data class Email(
    val emailAccountID: String,
    val recipients: List<Recipient>
) : Writeable, ToXContent {

    init {
        require(recipients.isNotEmpty()) { "At least one recipient must be provided" }
    }

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        return builder.startObject(TYPE)
            .field(EMAIL_ACCOUNT_ID_FIELD, emailAccountID)
            .field(RECIPIENTS_FIELD, recipients.toTypedArray())
            .endObject()
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeString(emailAccountID)
        out.writeCollection(recipients)
    }

    companion object {
        const val TYPE = "email"
        const val EMAIL_ACCOUNT_ID_FIELD = "email_account_id"
        const val RECIPIENTS_FIELD = "recipients"

        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): Email {
            lateinit var emailAccountID: String
            val recipients: MutableList<Recipient> = mutableListOf()

            ensureExpectedToken(Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    EMAIL_ACCOUNT_ID_FIELD -> emailAccountID = xcp.text()
                    RECIPIENTS_FIELD -> {
                        ensureExpectedToken(Token.START_ARRAY, xcp.currentToken(), xcp)
                        while (xcp.nextToken() != Token.END_ARRAY) {
                            recipients.add(Recipient.parse(xcp))
                        }
                    }
                    else -> {
                        throw IllegalStateException("Unexpected field: $fieldName, while parsing email destination")
                    }
                }
            }

            return Email(
                requireNotNull(emailAccountID) { "Email account ID is null" },
                recipients
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput): Email? {
            return if (sin.readBoolean()) {
                Email(
                    sin.readString(), // emailAccountID
                    sin.readList(::Recipient) // recipients
                )
            } else null
        }
    }
}

/**
 * A value object containing a recipient for an Email.
 */
data class Recipient(
    val type: RecipientType,
    val emailGroupID: String?,
    val email: String?
) : Writeable, ToXContent {

    init {
        when (type) {
            RecipientType.EMAIL_GROUP -> requireNotNull(emailGroupID) { "Email group ID is null" }
            RecipientType.EMAIL -> {
                requireNotNull(email) { "Email is null" }
                require(isValidEmail(email)) { "Invalid email" }
            }
        }
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readEnum(Recipient.RecipientType::class.java), // type
        sin.readOptionalString(), // emailGroupId
        sin.readOptionalString() // email
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject().field(TYPE_FIELD, type.value)

        when (type) {
            RecipientType.EMAIL_GROUP -> builder.field(EMAIL_GROUP_ID_FIELD, emailGroupID)
            RecipientType.EMAIL -> builder.field(EMAIL_FIELD, email)
        }

        return builder.endObject()
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeEnum(type)
        out.writeOptionalString(emailGroupID)
        out.writeOptionalString(email)
    }

    enum class RecipientType(val value: String) {
        EMAIL("email"),
        EMAIL_GROUP("email_group")
    }

    companion object {
        const val TYPE_FIELD = "type"
        const val EMAIL_GROUP_ID_FIELD = "email_group_id"
        const val EMAIL_FIELD = "email"

        @JvmStatic
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): Recipient {
            lateinit var type: String
            var emailGroupID: String? = null
            var email: String? = null

            ensureExpectedToken(Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    TYPE_FIELD -> {
                        type = xcp.text()
                        val allowedTypes = RecipientType.values().map { it.value }
                        if (!allowedTypes.contains(type)) {
                            throw IllegalStateException("Type should be one of $allowedTypes")
                        }
                    }
                    EMAIL_GROUP_ID_FIELD -> emailGroupID = xcp.text()
                    EMAIL_FIELD -> email = xcp.text()
                }
            }

            return Recipient(
                RecipientType.valueOf(type.toUpperCase(Locale.ROOT)),
                emailGroupID,
                email
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput): Recipient {
            return Recipient(sin)
        }
    }
}
