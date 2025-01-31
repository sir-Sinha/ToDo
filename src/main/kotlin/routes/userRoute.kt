package routes

import Client
import db.*
import connection.dbOperation
import connection.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*




fun Route.UserRoutes(dbObject:dbOperation){



    post("/createUser"){

        try{
            val userInput = call.receive<User>()
            val email = userInput.email
            val name = userInput.name
            val password = userInput.password

            if (email == null || name == null || password == null) {
                call.respond(HttpStatusCode.BadRequest, "Please Provide all information")
                return@post
            }

            if(Client.jedis.hexists("allEmail" ,email)){
                call.respond(HttpStatusCode.BadRequest , JsonErrorResponse("Fail" ,"$email already present"))
                return@post
            }
            Client.jedis.hset("allEmail" , email , password)
            dbObject.insert(User(email, name, password))


            val response = JsonResponse("success" , User(email, name, password))
            call.respond(HttpStatusCode.Created , response)
        } catch (ex: Exception) {
            val response = JsonErrorResponse("Fail" , "${ex.message}")
            call.respond(HttpStatusCode.InternalServerError, response)
        }
    }

    post("/deleteUser"){
        try{
            val userInput = call.receive<Credential>()
            val email = userInput.email
            val password = userInput.password


            if(!(Client.jedis.hexists("allEmail" ,email))){
                call.respond(HttpStatusCode.BadRequest , JsonErrorResponse("Fail" , "$email not exist"))
                return@post
            }

            Client.jedis.hdel("allEmail" , email)
            val setKey = "email:title:${userInput.email}"
            val valuesToDelete = Client.jedis.smembers(setKey)
            valuesToDelete.forEach { value ->
                Client.jedis.del("title:$value")
            }
            Client.jedis.del(setKey)
            Client.jedis.del("AllTodo:${userInput.email}")


            val b = dbObject.deleteUser(email,password)

            if(b) {
                call.respond(HttpStatusCode.OK , JsonSuccess("success"))
            }
            else call.respond(HttpStatusCode.BadRequest, JsonErrorResponse("Error" , "$email don't exist or password is incorrect"))
        }catch (e:Exception){
            val response = JsonErrorResponse("Fail" , "${e.message}")
            call.respond(HttpStatusCode.InternalServerError , response)
        }
    }

    put("/updateUser"){
        try{
            val input = call.receive<UserUpdate>()

            val name = call.request.queryParameters["name"]
            val password = call.request.queryParameters["password"]

            dbObject.Check(input.email,input.password)
            var flag = false
            if(name != null){
                flag =true
                dbObject.updateName(input.email,name)
            }
            if(password != null){
                flag = true
                Client.jedis.hset("allEmail" , input.email , password)
                dbObject.updatePassword(input.email,password)

            }

            if(flag) call.respond(HttpStatusCode.OK  , JsonSuccess("success"))
            else call.respond(HttpStatusCode.Forbidden , JsonSuccess("Nothing Updated"))


        }catch (e : Exception){
            val response = JsonErrorResponse("Fail" , "${e.message}")
            call.respond(HttpStatusCode.InternalServerError , response)
        }
    }



}