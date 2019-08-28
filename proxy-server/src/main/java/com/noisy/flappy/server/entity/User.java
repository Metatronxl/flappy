package com.noisy.flappy.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lei.X
 * @date 2019/8/28
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    int id;
    String username;
    String password;

}
