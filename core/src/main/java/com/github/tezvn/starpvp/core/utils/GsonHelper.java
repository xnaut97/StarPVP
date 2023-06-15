package com.github.tezvn.starpvp.core.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GsonHelper {

	public static String encode(Object obj) {
		return new Gson().toJson(obj);
	}
	
	public static Object decode(String args){
		return new Gson().fromJson(args, new TypeToken<Object>() {}.getType());
	}
	
}
