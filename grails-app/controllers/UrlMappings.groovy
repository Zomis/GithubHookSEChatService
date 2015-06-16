class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
        "/hook"(controller: "githubHook", action: "hook")
        "/hooks/appveyor"(controller: "appveyorHook", action: "build")
        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
