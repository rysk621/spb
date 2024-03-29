package com.project.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.dto.ReportDTO;
import com.project.service.DataService;
import com.project.util.WorkerEmul;

import lombok.RequiredArgsConstructor;

@EnableAsync
@RestController
@RequiredArgsConstructor
public class DataController {
	private final WorkerEmul wEmul;
	private final DataService dataServ;

	// 에뮬레이터 생성 정보 수신, log repo 저장 --> DA에서 분석, status cur repo 저장 (10초 주기)
	@Async
//	@Scheduled(initialDelay = 0, fixedRate = 10000)
	public void scheduledDataProcess() throws IOException {
		List<ReportDTO> wList = wEmul.get(); // wList size == 20;

		dataServ.receiveData(wList);
	}

	// WS 이용, data push to FE (10초 주기)
	@Async
	@Scheduled(initialDelay = 1000, fixedRate = 10000)
	public void dataPushProcess() throws IOException {
		dataServ.pushData();
	}
}