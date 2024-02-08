package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.entities.SmallInfo;
import com.smalltalk.SmallTalkFootball.services.SmallInfoService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("small-infos")
public class SmallInfoController {

    private final SmallInfoService service;

    @PostMapping
    public SmallInfo addSmallInfos(@RequestBody SmallInfo smallInfo) {
        return service.addInfo(smallInfo);
    }

    @GetMapping
    public List<SmallInfo> getAllInfo() {
        return service.getAllInfos();
    }

    @GetMapping("/{id}")
    public SmallInfo getOneSmallInfo(@PathVariable String id) throws Exception {
        return service.getOneInfo(id);
    }
}
