package com.project.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.project.domain.Worker;
import com.project.domain.WorkerLog;
import com.project.domain.dto.WorkerInitDTO;
import com.project.domain.dto.WorkerLogDTO;
import com.project.persistence.WorkerCurRepository;
import com.project.persistence.WorkerLogRepository;
import com.project.persistence.WorkerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
	private final WorkerRepository workerRepo;
	private final WorkerLogRepository workerLogRepo;
	private final WorkerCurRepository workerCurRepo;

	public ResponseEntity<?> findWorker() {
		try {
			List<Worker> list = workerRepo.findAll();
			List<WorkerInitDTO> initList = new ArrayList<>();
			for(Worker w : list) {
				int id = w.getId();
				List<WorkerLog> wList = workerLogRepo.findInitData(id);
				if(wList.isEmpty()) return ResponseEntity.noContent().build();

				// 로그 register date 순서로 sorting
				Comparator<WorkerLog> compareByRegidate = new Comparator<WorkerLog>() {
					@Override
					public int compare(WorkerLog o1, WorkerLog o2) {
						return o1.getRegisterDate().compareTo(o2.getRegisterDate());
					}
				};
				Collections.sort(wList, compareByRegidate);

				WorkerInitDTO idto = WorkerInitDTO.builder()
						.id(id)
						.workerName(w.getWorkerName())
						.department(w.getDepartment())
						.regidate(w.getRegidate())
						.status(workerCurRepo.findById(id).get().getStatus())
						.role(w.getRole())
						.build();
				for(WorkerLog wl : wList) {
					WorkerLogDTO logdto = WorkerLogDTO.builder()
							.latitude(wl.getLatitude())
							.longitude(wl.getLongitude())
							.heartbeat(wl.getHeartbeat())
							.temperature(wl.getTemperature())
							.outTemp(wl.getOutTemp())
							.build();
					idto.setYear(Year.now().getValue() - w.getYear()); // 생년 --> 나이

					idto.getList().add(logdto);
				}
				initList.add(idto);
			}
			return ResponseEntity.ok().body(initList);
		} catch (Exception e) {
			log.warn("Exception at WorkerService.findWorker: {}", e.toString());
			return ResponseEntity.badRequest().body("!!! findWorker Exception: " + e.toString());
		}
	}

	public ResponseEntity<?> findWorkerById(int id) {
		try {
			return ResponseEntity.ok().body(workerRepo.findById(id).get());
		} catch (Exception e) {
			log.warn("Exception at WorkerService.findWorkerById: {}", e.toString());
			return ResponseEntity.badRequest().body("!!! findWorkerById Exception: " + e.toString());
		}
	}
}
