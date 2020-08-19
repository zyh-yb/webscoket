package com.example.webscoket.demo.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * zyh
 * 2020/07/29
 */
@Controller
public class Index {
    @RequestMapping("/")
    public String index(){
        return "index";
    }
}
