package com.adem.attijari_compass.config;

import com.adem.attijari_compass.entity.CardSourceType;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.service.card.CardTransactionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 80)
@Slf4j
public class LinkedUserCardTransactionSyncInitializer implements CommandLineRunner {

    private final UserCardRepository userCardRepository;
    private final CardTransactionSyncService cardTransactionSyncService;

    @Override
    @Transactional
    public void run(String... args) {
        int cardsChecked = 0;
        int inserted = 0;
        int updated = 0;

        for (UserCard userCard : userCardRepository.findActiveManagedCardsWithPoolBySourceType(CardSourceType.DEMO_POOL)) {
            if (userCard.getCardPool() == null) {
                continue;
            }

            cardsChecked++;
            CardTransactionSyncService.TransactionSyncResult result =
                    cardTransactionSyncService.syncPoolTransactions(userCard, userCard.getCardPool());
            inserted += result.inserted();
            updated += result.updated();
        }

        log.info(
                "Linked user card transactions synchronized: {} cards checked, {} inserted, {} updated",
                cardsChecked,
                inserted,
                updated
        );
    }
}
