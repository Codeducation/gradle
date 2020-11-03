package com.myorg.myproduct.server;

import com.myorg.myproduct.user.table.TableBuilder;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MyProductJsonController {

    @RequestMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<List<String>> json() {
        return TableBuilder.build();
    }

}