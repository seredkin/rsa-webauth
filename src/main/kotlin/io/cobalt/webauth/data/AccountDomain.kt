package io.cobalt.webauth.data

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.singleOrNone
import io.cobalt.webauth.data.TAccountPasswordReset.emailToken
import io.cobalt.webauth.rest.AccountExists
import io.cobalt.webauth.rest.AccountFree
import io.cobalt.webauth.rest.GoodBye
import io.cobalt.webauth.rest.SignInRequest
import io.cobalt.webauth.rest.SignInResponse
import io.cobalt.webauth.rest.SignUpRequest
import io.cobalt.webauth.rest.Welcome
import io.micronaut.context.annotation.Infrastructure
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.UserDetails
import io.micronaut.security.authentication.providers.UserState
import io.reactivex.Flowable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.sql.DataSource
import java.util.Base64.getDecoder as decoder64
import java.util.Base64.getEncoder as encoder64


internal interface AccountDomainTable {
    val uuid: Column<UUID>
    val createdAt: Column<DateTime>
}

private object TAccountPasswordReset : Table("account_password_reset"), AccountDomainTable {
    override val uuid = uuid("uuid").primaryKey()
    val email = text("email").index()
    override val createdAt = datetime("created_at")
    val emailToken = text("email_token").uniqueIndex()
    val resetToken = text("reset_token").uniqueIndex()
    val confirmedAt = datetime("confirmed_at").nullable()
}

private object TAccountGroup : Table("account_group"), AccountDomainTable {
    override val uuid = uuid("uuid").primaryKey()
    val groupName = text("group_name")
    override val createdAt = datetime("created_at")
}

private object TAccount : Table("account"), AccountDomainTable {
    override val uuid = uuid("uuid").primaryKey()
    val groupUuid = uuid("group_uuid").references(TAccountGroup.uuid)
    val email = text("email").uniqueIndex()
    val customerName = text("customer_name")
    val customerContacts = text("customer_contacts")
    val salt = text("salt")
    val passwordHash = text("passwordHash")
    override val createdAt = datetime("created_at")
    val lastPasswordChange = datetime("last_password_change")
}

private object TAccountLoginAttempt : Table("account_login_attempt"), AccountDomainTable {
    override val uuid = uuid("uuid").primaryKey()
    val email = text("email")
    val ip = text("ip")
    val suspended = bool("suspended")
    val success = bool("success")
    override val createdAt = datetime("created_at")
}

private object TAccountCreationAttempt : Table("account_create_attempt"), AccountDomainTable {
    override val uuid = uuid("uuid").primaryKey()
    val email = text("email")
    val customerName = text("customer_name")
    val customerContacts = text("customer_contacts")
    val ip = text("ip")
    val suspended = bool("suspended")
    val confirmed = bool("confirmed")
    val confirmedAt = datetime("confirmed_at").nullable()
    val confirmationToken = text("confirmation_token")
    val success = bool("success")
    val salt = binary("salt", 128)
    val passwordHash = text("password_hash")
    override val createdAt = datetime("created_at")
}

@Infrastructure
class AccountRepo(private val dataSource: DataSource) {

    fun fetchAccountGroup(gName: String): UUID = transaction(db()) {
        with(TAccountGroup) {
            select { groupName eq gName }.singleOrNone().map { it[uuid] }.getOrElse { error("AccountGroup not found") }
        }
    }

    fun signUpAttempt(email: String): Either<AccountFree, AccountExists> =

            Either.cond(accountExists(email), { AccountExists(email) }, { AccountFree(email) })

    fun singInAttempt(email: String, password: String): Either<Welcome, GoodBye> = transaction(db()) {
        with(TAccount) {
            select { TAccount.email eq email }.singleOrNone().map { }
        }

        Either.right(GoodBye("TEST STUB"))
    }

    //TODO:Anton Input validation
    private fun createAccount(attempt: ResultRow): Option<Welcome> = transaction {
        Try {
        TAccount.insert {
            it[uuid] = UUID.randomUUID()
            it[email] = attempt[TAccountCreationAttempt.email]
            it[passwordHash] = attempt[TAccountCreationAttempt.passwordHash]
            it[salt] = attempt[TAccountCreationAttempt.passwordHash]
            it[customerName] = attempt[TAccountCreationAttempt.customerName]
            it[customerContacts] = attempt[TAccountCreationAttempt.customerContacts]
            it[groupUuid] = fetchAccountGroup("ROLE_USER")
            it[lastPasswordChange] = DateTime.now()
            it[createdAt] = DateTime.now()
        }
            Option.just(Welcome("Account verified. Proceed to the Login page"))
        }.getOrElse { error("Error verifying account") }
    }


    private val db = { Database.connect(dataSource) }

    internal fun accountExists(email: String) = transaction(db()) {
        TAccount.select { TAccount.email eq email }.singleOrNone().nonEmpty()
    }

    fun passwordReset(emailTokenStr: String): Either<GoodBye, Welcome> = transaction(db()) {
        val pwdReset =
                with(TAccountPasswordReset) {
                    select { emailToken eq emailTokenStr }
                            .singleOrNone().map { rowToPasswordReset(it) }
                }
        when {
            pwdReset.isEmpty() -> Either.left(GoodBye("Token unknown"))
            pwdReset.filter { it.confirmedAt.nonEmpty() }.nonEmpty() -> Either.left(GoodBye("Token already used"))
            else -> {
                TAccountPasswordReset.update( {
                    emailToken eq emailTokenStr
                }) {
                    it[this.confirmedAt] = DateTime.now()
                }
                Either.right(Welcome("Password reset confirmed. Please check your email."))
            }
        }

    }

    fun rowToPasswordReset(row: ResultRow) = with(TAccountPasswordReset) {
        PasswordReset(
                email = row[email],
                token = row[emailToken],
                createdAt = Instant.ofEpochMilli(row[createdAt].millis),
                confirmedAt = Option.fromNullable(row[confirmedAt]).map { Instant.ofEpochMilli(it.millis) }
        )
    }

    fun signup(sr: SignUpRequest, emailToken: String): Welcome {
        val signupUuid = UUID.randomUUID()
        val salt = randomBytes(128)
        transaction(db()) {
            with(TAccountCreationAttempt) {
                insert {
                    it[uuid] = signupUuid
                    it[email] = sr.email
                    it[customerName] = sr.name
                    it[customerContacts] = sr.contacts
                    it[confirmed] = false
                    it[confirmedAt] = null
                    it[confirmationToken] = emailToken
                    it[suspended] = true
                    it[success] = false
                    it[this.salt] = salt
                    it[passwordHash] = hashPassword(sr.password, salt)
                    it[ip] = "127.0.0.1"
                }
            }
        }

        return Welcome("TODO: ${sr.email}")
    }

    private fun randomBytes(size: Int): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(size)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPassword(pwd: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pwd.toCharArray(), salt, 65536, 160)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return encoder64().encodeToString(factory.generateSecret(spec).encoded)
    }

    fun confirmEmail(emailToken: String): Option<Welcome> = transaction(db()) {
        with(TAccountCreationAttempt) {
            select { confirmationToken eq emailToken }.singleOrNone().map { row ->
                update({ uuid eq row[uuid] }) {
                    it[confirmed] = true
                    it[suspended] = false
                    it[success] = true
                    it[confirmedAt] = DateTime.now()
                }
                createAccount(row)
            }.getOrElse { error("Error verifying email") }
        }
    }

    fun findByEmail(username: String): Option<UserState> = transaction(db()) {
        with(TAccount) {
            select {
                email eq email
            }.singleOrNone().map {
                rowToWebAuthUserState(it)
            }
        }
    }

    private fun TAccount.rowToWebAuthUserState(it: ResultRow): WebAuthUserState {
        return WebAuthUserState(
                email = it[email],
                passwordHash = encoder64().encodeToString(it[passwordHash].toByteArray()),
                suspended = false,
                salt = encoder64().encodeToString(it[salt].toByteArray())
        )
    }

    fun signIn(sr: SignInRequest): Either<GoodBye, SignInResponse> = transaction(db()) {
        with(TAccount) {
            select { email eq sr.email }.singleOrNone()
                    .filter {
                        it[passwordHash] == hashPassword(sr.password, decoder64().decode(it[salt]))
                    }.map {
                        val sessionToken = encoder64().encodeToString(randomBytes(4096))
                        SignInResponse(sr.email, sessionToken, Instant.now())
                    }.toEither { GoodBye("Credentials don't match") }
        }
    }

    fun authenticate(request: AuthenticationRequest<*, *>): UserDetails {
        //TODO Anton check pwd hash against DB
        val password = request.secret  as String
        val email  = request.identity as String
        return UserDetails(email, listOf("ROLE_USER"))
    }

    data class WebAuthUserState(
            val salt: String,
            private val email: String,
            private val passwordHash: String,
            private val suspended: Boolean

    ) : UserState {
        override fun isEnabled() = suspended
        override fun isPasswordExpired() = suspended
        override fun getUsername() = email
        override fun isAccountExpired() = suspended
        override fun getPassword() = passwordHash
        override fun isAccountLocked() = suspended
    }

    data class PasswordReset(
            val email: String,
            val token: String,
            val createdAt: Instant,
            val confirmedAt: Option<Instant>
    )
}
