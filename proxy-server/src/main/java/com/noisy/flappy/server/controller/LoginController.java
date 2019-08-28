package com.noisy.flappy.server.controller;

import com.noisy.flappy.server.entity.Result;
import com.noisy.flappy.server.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Objects;

/**
 * @author lei.X
 * @date 2019/8/28
 */

@RestController
@RequestMapping("api")
public class LoginController {

    @CrossOrigin
    @PostMapping(value = "login")
    @ResponseBody
    public Result login(@RequestBody User requestUser) {
        // 对 html 标签进行转义，防止 XSS 攻击
        String username = requestUser.getUsername();
        username = HtmlUtils.htmlEscape(username);

        if (!Objects.equals("admin", username) || !Objects.equals("123456", requestUser.getPassword())) {
            String message = "账号密码错误";
            return new Result(400);
        } else {
            return new Result(200);
        }
    }


    @GetMapping(value = "test")
    @ResponseBody
    public Result test() {
        // 对 html 标签进行转义，防止 XSS 攻击
        return new Result(400);
    }



}
