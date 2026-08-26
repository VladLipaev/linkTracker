//package backend.academy.linktracker.scrapper.service;
//
//import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
//import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.OffsetDateTime;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class OutboxDaoService {
//
//    private final OutBoxRepository outBoxRepository;
//
//    @Transactional
//    public List<OutBoxMessage> findNewMessagesWithLock(Integer batchSendSize, Integer maxRetries){
//        return outBoxRepository.findNewWithLock(batchSendSize, maxRetries);
//    }
//
//    @Transactional
//    public void updateMessages(List<UUID> sentMessagesIds, List<UUID> errorMessagesIds) {
//        OffsetDateTime now = OffsetDateTime.now();
//        if (!sentMessagesIds.isEmpty()) {
//            outBoxRepository.updateSentMessages(sentMessagesIds, now);
//        }
//        if (!errorMessagesIds.isEmpty()) {
//            outBoxRepository.updateErrorMessages(errorMessagesIds, now);
//        }
//    }
//
//    @Transactional
//    public int cleanUp(OffsetDateTime threshold, Integer cleanSize){
//        return outBoxRepository.cleanUpBatch(threshold, cleanSize);
//    }
//}
