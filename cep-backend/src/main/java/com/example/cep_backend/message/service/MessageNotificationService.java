package com.example.cep_backend.message.service;

import com.example.cep_backend.message.repository.MessageNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MessageNotificationService {
    private final MessageNotificationRepository messageNotificationRepository;

    public MessageNotificationService(MessageNotificationRepository messageNotificationRepository) {
        this.messageNotificationRepository = messageNotificationRepository;
    }

    @Transactional
    public void notifyItemFavorited(Long itemId, Long ownerUserId, Long actorUserId) {
        if (itemId == null || ownerUserId == null || actorUserId == null) {
            return;
        }
        if (ownerUserId <= 0 || actorUserId <= 0 || ownerUserId.equals(actorUserId)) {
            return;
        }

        String actorName = messageNotificationRepository.findUsernameByUserId(actorUserId);
        String itemTitle = messageNotificationRepository.findItemTitle(itemId);
        String title = "商品被收藏提醒";
        String content = actorName + " 收藏了你的商品《" + itemTitle + "》";
        messageNotificationRepository.insertNotification(
                ownerUserId,
                "ITEM_FAVORITED",
                title,
                content,
                itemId,
                actorUserId,
                LocalDateTime.now());
    }

    @Transactional
    public void notifyFollowed(Long targetUserId, Long followerUserId) {
        if (targetUserId == null || followerUserId == null) {
            return;
        }
        if (targetUserId <= 0 || followerUserId <= 0 || targetUserId.equals(followerUserId)) {
            return;
        }

        String followerName = messageNotificationRepository.findUsernameByUserId(followerUserId);
        String title = "被关注提醒";
        String content = followerName + " 关注了你";
        messageNotificationRepository.insertNotification(
                targetUserId,
                "FOLLOWED",
                title,
                content,
                null,
                followerUserId,
                LocalDateTime.now());
    }

    @Transactional
    public void notifyFavoritePriceDrop(Long itemId, Long ownerUserId, BigDecimal oldPrice, BigDecimal newPrice) {
        if (itemId == null || ownerUserId == null || oldPrice == null || newPrice == null) {
            return;
        }
        if (itemId <= 0 || ownerUserId <= 0 || newPrice.compareTo(oldPrice) >= 0) {
            return;
        }

        String itemTitle = messageNotificationRepository.findItemTitle(itemId);
        String title = "收藏商品降价提醒";
        String content = "你收藏的《" + itemTitle + "》已降价：¥" + oldPrice + " → ¥" + newPrice;
        messageNotificationRepository.insertPriceDropNotifications(
                itemId,
                ownerUserId,
                title,
                content,
                LocalDateTime.now());
    }
}
