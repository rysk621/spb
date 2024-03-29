package com.project.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.domain.Admin;
import com.project.persistence.AdminRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final AdminRepository adminRepo;
	private final PasswordEncoder encoder;

	public void addAdmin(String id) {
		Admin ad = Admin.builder().id(id).password(encoder.encode("1234")).build();
		adminRepo.saveAndFlush(ad);
	}
}
