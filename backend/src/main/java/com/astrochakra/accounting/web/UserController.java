package com.astrochakra.accounting.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.astrochakra.accounting.service.UserService;
import com.astrochakra.accounting.web.dto.CreateUserRequest;
import com.astrochakra.accounting.web.dto.UpdateUserRequest;
import com.astrochakra.accounting.web.dto.UserDto;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserDto> list() {
        return service.list();
    }

    @PostMapping
    public UserDto create(@RequestBody CreateUserRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
