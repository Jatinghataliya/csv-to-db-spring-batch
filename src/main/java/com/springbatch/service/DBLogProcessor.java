package com.springbatch.service;

import org.springframework.batch.item.ItemProcessor;

import com.springbatch.dao.Hotels;

public class DBLogProcessor implements ItemProcessor<Hotels, Hotels> {

	@Override
	public Hotels process(Hotels item) throws Exception {
		System.out.println("Hotels : " + item.toString());
		return item;
	}

}