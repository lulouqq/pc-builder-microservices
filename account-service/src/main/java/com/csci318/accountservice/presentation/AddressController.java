package com.csci318.accountservice.presentation;

import com.csci318.accountservice.service.UserService;
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

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/addresses")
public class AddressController {

    private final UserService userService;

    public AddressController(UserService userService) {
        this.userService = userService;
    }

    /** A1: retrieve saved delivery addresses. */
    @GetMapping
    public List<AddressResponse> getAddresses(@PathVariable String userId) {
        return userService.getAddresses(userId).stream().map(AddressResponse::from).toList();
    }

    /** A1: add a saved delivery address. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@PathVariable String userId, @RequestBody AddressCreateRequest request) {
        return AddressResponse.from(userService.addAddress(userId, request.toCommand()));
    }

    /** A1: update a saved delivery address. */
    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(@PathVariable String userId, @PathVariable String addressId,
                                         @RequestBody AddressUpdateRequest request) {
        return AddressResponse.from(userService.updateAddress(userId, addressId, request.toCommand()));
    }

    /** A1: remove a saved delivery address. */
    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAddress(@PathVariable String userId, @PathVariable String addressId) {
        userService.removeAddress(userId, addressId);
    }
}
