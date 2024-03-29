package com.project.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.config.MyWebSocketConfig;
import com.project.domain.PreexLog;
import com.project.domain.Worker;
import com.project.domain.WorkerCur;
import com.project.domain.WorkerLog;
import com.project.domain.dto.ReportDTO;
import com.project.persistence.PreexLogRepository;
import com.project.persistence.WorkerCurRepository;
import com.project.persistence.WorkerLogRepository;
import com.project.persistence.WorkerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {
	private int num = 10; // offset number
	private final String postLog = "http://10.125.121.170:5000/postLog/";
	@SuppressWarnings("unused")
	private final String postLogUser = "http://10.125.121.155:5000/postLog/"; // 주소 변경

	private final WorkerRepository workerRepo;
	private final WorkerLogRepository workerLogRepo;
	private final WorkerCurRepository workerCurRepo;
	private final PreexLogRepository preRepo;
	private final RestTemplate restTemp;
	private final WebClient client;
	private final ObjectMapper om = new ObjectMapper();

	// 에뮬레이터에서 생성된 정보 전달받아 WorkerLog table에 저장 --> DA에 각 작업자별 로그 전달, 반환된 status 저장
	public void receiveData(List<ReportDTO> dtoList) {
		// 1~96 ReportDTO 전달받아 리스트에 저장 --> 리스트 workerLogRepo에 save and flush
		List<WorkerLog> list = new ArrayList<>();
		for (ReportDTO dto : dtoList) {
			list.add(WorkerLog.builder().usercode(dto.getUsercode()).latitude(dto.getLatitude())
					.longitude(dto.getLongitude()).heartbeat(dto.getHeartbeat()).temperature(dto.getTemperature())
					.outTemp(dto.getOutTemp()).build());
		}
		workerLogRepo.saveAllAndFlush(list);

		// 각 작업자별 로그(dummy log) DA에 전달 --> 분석된 status 반환 받음
		for(Worker w : workerRepo.findAll()) {
			int userCode = w.getId();
			log.info("> data posted ({})", userCode);
			List<WorkerLog> logList = workerLogRepo.findData(userCode, num);
			WorkerCur wC = workerCurRepo.findById(userCode).get();

			try {
				URI uri = new URL("http","10.125.121.170", 5000, "/postLog/" + userCode).toURI();
				WorkerCur postedByWC = client
						.post()
						.uri(uri).accept(MediaType.APPLICATION_JSON).bodyValue(logList)
						.retrieve().bodyToMono(WorkerCur.class).block();
				wC.setStatus(postedByWC.getStatus());

				// 세팅한 workerCur repository에 저장, 종료
				workerCurRepo.saveAndFlush(wC);
				log.info(
						">>> usercode " + userCode + " state: " + wC.getStatus().toString() + ", DA success\n");
				if(userCode == 96) num += 10;
			} catch (Exception e) {
				log.warn("!!! {} postData Exception : {}", userCode, e.toString());
			}
		}
	}

	// 에뮬레이터에서 생성된 정보 전달받아 WorkerLog table에 저장
	public void receiveDataPreLog(List<ReportDTO> dtoList) {
		// 1~96 ReportDTO 전달받아 리스트에 저장 --> 리스트 workerLogRepo에 save and flush
		List<WorkerLog> list = new ArrayList<>();
		for (ReportDTO dto : dtoList) {
			list.add(WorkerLog.builder().usercode(dto.getUsercode()).latitude(dto.getLatitude())
					.longitude(dto.getLongitude()).heartbeat(dto.getHeartbeat()).temperature(dto.getTemperature())
					.outTemp(dto.getOutTemp()).build());
		}
		workerLogRepo.saveAllAndFlush(list);

		// 각 작업자별 로그(pre-existing log) DA에 전달 --> 분석된 status 반환 받음
		for(Worker w : workerRepo.findAll()) {
			int userCode = w.getId();
			if(log.isDebugEnabled()) {
				log.debug("> data posted({})", userCode);
			}
			List<PreexLog> logList = preRepo.findData(userCode, num);
			WorkerCur wC = workerCurRepo.findById(userCode).get();

			try {
				// header 정보에 MediaType = JSON 설정 후 전달
				final HttpHeaders header = new HttpHeaders();
				header.setContentType(MediaType.APPLICATION_JSON);
				final HttpEntity<?> entity = new HttpEntity<>(logList, header);

				// exchange method 이용, method 설정(get/post/put/delete/etc) --> WorkerCur에 return
				// 정보 저장
				WorkerCur posted = restTemp.exchange(postLog + userCode, HttpMethod.POST, entity, WorkerCur.class)
						.getBody();
				wC.setStatus(posted.getStatus());

				// 세팅한 workerCur repository에 저장, 종료
				workerCurRepo.saveAndFlush(wC);
				log.info(
						">>> usercode {} state: {}, DA success", userCode, wC.getStatus().toString());
				if(userCode == 96) num += 10;
			} catch (Exception e) {
				log.warn("!!! {} postData Exception : {}", userCode, e.toString());
			}
		}
	}

	// WS 이용, push data to FE
	public void pushData() throws IOException {
		List<WorkerCur> wList = workerCurRepo.findAll();
		if (MyWebSocketConfig.clients.size() == 0) { // 연결된 클라이언트가 없으면 그냥 리턴
			if(log.isDebugEnabled()) {
				log.debug("FE no client connection.");
			}
			return;
		}

		try {
			String msg = om.writeValueAsString(wList);
			log.info("> data push to FE\tw(pushed to front)"); // 콘솔 확인
			TextMessage message = new TextMessage(msg.getBytes());

			for (WebSocketSession sess : MyWebSocketConfig.clients) {
				sess.sendMessage(message);
			}
		} catch (Exception e) {
			String msg = e.toString();
			TextMessage message = new TextMessage(msg.getBytes());

			for (WebSocketSession sess : MyWebSocketConfig.clients) {
				sess.sendMessage(message);
			}
			log.warn("!!! pushData 메서드 Exception : {}", e.toString());
		}
	}
}
