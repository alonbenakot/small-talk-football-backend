package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.entities.SmallInfo;
import com.smalltalk.SmallTalkFootball.services.SmallInfoService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("small-infos")
public class SmallInfoController {

    private final SmallInfoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<SmallInfo> addSmallInfos(@RequestBody SmallInfo smallInfo) {
        return service.addInfo(smallInfo);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<List<SmallInfo>> getAllInfos() {
        return service.getAllInfos();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<SmallInfo> getOneSmallInfo(@PathVariable String id) throws SmallTalkException {
        return service.getOneInfo(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteSmallInfo(@PathVariable String id) throws SmallTalkException {
        service.deleteSmallInfo(id);
    }
}
