/**
 * Created by Alexandre Rio on 12/12/17.
 */
import com.google.gson.Gson
import groovy.util.Eval
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.KClass

val gson = Gson()

fun main(args: Array<String>) {
    val tokens = mutableMapOf<String, LocalDateTime>()
    val port = 8080
    val vertx = Vertx.vertx()
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())

    // Login
    router.post("/login").handler({ ctx ->
        val user = jsonRequest<User>(ctx, User::class)

        if (user?.login.equals("admin") && user?.pass.equals("admin")) {
            val uid = UUID.randomUUID().toString()
            tokens.put(uid, LocalDateTime.now())
            jsonResponse(ctx, Future.succeededFuture(Token(uid)))
        } else {
            jsonResponse(ctx, Future.succeededFuture(Fault("invalid login and password")))
        }
    })

    // Evaluation
    router.post("/compute").handler({ ctx ->
        var rq = jsonRequest<Compute>(ctx, Compute::class)
        val tokenCreation = tokens.get(rq?.token)
        if (tokenCreation != null && ChronoUnit.MINUTES.between(tokenCreation, LocalDateTime.now()) < 15) {
            jsonResponse(ctx, Future.succeededFuture(Result(Eval.me(rq?.expression))))
        } else {
            tokens.remove(rq?.token)
            jsonResponse(ctx, Future.succeededFuture(Fault("invalid token")))
        }
    })


    server.requestHandler { router.accept(it) }.listen(port) {
        if (it.succeeded()) println("Server listening at $port")
        else println(it.cause())
    }
}

fun <T> jsonRequest(ctx: RoutingContext, clazz: KClass<out Any>): T? =
        gson.fromJson(ctx.bodyAsString, clazz.java) as T?


fun <T> jsonResponse(ctx: RoutingContext, future: Future<T>) {
    future.setHandler {
        if (it.succeeded()) {
            val res = if (it.result() == null) "" else gson.toJson(it.result())
            ctx.response().end(res)
        } else {
            ctx.response().setStatusCode(500).end(it.cause().toString())
        }
    }
}

data class User(val login: String, val pass: String)
data class Compute(val token:String, val expression:String)
data class Result(val result:Any)
data class Token(val token: String)
data class Fault(val fault: String)