package net.zomis.duga

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment

class GithubHookController {

    static allowedMethods = [hook:'POST']

    @Autowired
    DugaBot bot

    @Autowired
    Environment environment

    def test() {
        render 'Hello World'
    }

    def message() {
        def value = environment.getProperty('botName')
        bot.postChat('This is a test, ' + value)
        render 'Posted'
    }

    def hook() {
        println 'JSON Data: ' + params
        println 'JSON Request: ' + request.JSON
        println 'Request: ' + request
        println 'Github Event: ' + request.getHeader('X-GitHub-Event')
        render 'OK'
    }

}
