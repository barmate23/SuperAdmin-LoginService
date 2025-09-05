package com.AccessManagement.Config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.NotNull;
import lombok.Data;



@Data
public class JwtRequest {
	@JsonProperty(required = true)
	private String username;

	@JsonProperty(required = true)
	private String password;

}
