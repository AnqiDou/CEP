package com.example.cep_backend.profile.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.auth.repository.UserRepository;
import com.example.cep_backend.message.service.MessageNotificationService;
import com.example.cep_backend.profile.dto.ProfileOverviewDto;
import com.example.cep_backend.profile.dto.ProfileAvatarUploadDto;
import com.example.cep_backend.profile.dto.OtherProfileItemDto;
import com.example.cep_backend.profile.dto.OtherProfileOverviewDto;
import com.example.cep_backend.profile.dto.OtherProfileReviewItemDto;
import com.example.cep_backend.profile.dto.OtherProfileReviewSummaryDto;
import com.example.cep_backend.profile.dto.ProfilePendingTradeDto;
import com.example.cep_backend.profile.dto.ProfileReviewItemDto;
import com.example.cep_backend.profile.dto.ProfileReviewSummaryDto;
import com.example.cep_backend.profile.dto.ProfileTradeItemDto;
import com.example.cep_backend.profile.dto.ProfileUpdateRequest;
import com.example.cep_backend.profile.repository.ProfileRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileService {
    private static final String ROLE_SELLER = "SELLER";
    private static final String ROLE_BUYER = "BUYER";
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileImageService profileImageService;
    private final MessageNotificationService messageNotificationService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ProfileService(ProfileRepository profileRepository,
            UserRepository userRepository,
            ProfileImageService profileImageService,
            MessageNotificationService messageNotificationService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.profileImageService = profileImageService;
        this.messageNotificationService = messageNotificationService;
    }

    public ProfileOverviewDto getOverview(Long userId) {
        profileRepository.ensureUserProfile(userId);
        ProfileRepository.ProfileBaseInfo baseInfo = profileRepository.findBaseInfo(userId);
        if (baseInfo == null) {
            throw new BusinessException("用户不存在");
        }
        ProfileRepository.CreditStats sellerStats = profileRepository.findCreditStats(userId, ROLE_SELLER);
        ProfileRepository.CreditStats buyerStats = profileRepository.findCreditStats(userId, ROLE_BUYER);
        return new ProfileOverviewDto(
                baseInfo.avatar(),
                baseInfo.username(),
                baseInfo.fans(),
                baseInfo.following(),
                resolveCreditLabel(sellerStats),
                resolveCreditLabel(buyerStats));
    }

    public ProfileReviewSummaryDto getReviews(Long userId, String rating) {
        profileRepository.ensureUserProfile(userId);
        String normalized = normalizeRating(rating);
        ProfileRepository.CreditStats sellerStats = profileRepository.findCreditStats(userId, ROLE_SELLER);
        ProfileRepository.CreditStats buyerStats = profileRepository.findCreditStats(userId, ROLE_BUYER);
        int totalGood = sellerStats.goodCount() + buyerStats.goodCount();
        int totalBad = sellerStats.badCount() + buyerStats.badCount();
        List<com.example.cep_backend.profile.dto.ProfileReviewItemDto> reviews = profileRepository.findReviews(userId,
                normalized);
        return new ProfileReviewSummaryDto(totalGood + totalBad, totalGood, totalBad, reviews);
    }

    public List<ProfileTradeItemDto> getPublishedItems(Long userId) {
        return profileRepository.findPublishedItems(userId);
    }

    public List<ProfileTradeItemDto> getSoldItems(Long userId) {
        return profileRepository.findSoldItems(userId);
    }

    public List<ProfileTradeItemDto> getBoughtItems(Long userId) {
        return profileRepository.findBoughtItems(userId);
    }

    public List<ProfileTradeItemDto> getFavoriteItems(Long userId) {
        return profileRepository.findFavoriteItems(userId);
    }

    public List<ProfilePendingTradeDto> getPendingPaymentTrades(Long userId) {
        return profileRepository.findPendingPaymentTrades(userId);
    }

    public OtherProfileOverviewDto getOtherOverview(Long viewerUserId, Long userId, String username) {
        Long targetUserId = resolveTargetUserId(userId, username);
        profileRepository.ensureUserProfile(targetUserId);
        ProfileRepository.OtherProfileBaseInfo baseInfo = profileRepository.findOtherProfileBase(targetUserId);
        if (baseInfo == null) {
            throw new BusinessException("用户不存在");
        }
        ProfileRepository.CreditStats sellerStats = profileRepository.findCreditStats(targetUserId, ROLE_SELLER);
        ProfileRepository.CreditStats buyerStats = profileRepository.findCreditStats(targetUserId, ROLE_BUYER);
        boolean followed = profileRepository.isFollowing(viewerUserId, targetUserId);
        return new OtherProfileOverviewDto(
                targetUserId,
                baseInfo.avatar(),
                baseInfo.username(),
                baseInfo.city(),
                baseInfo.fans(),
                baseInfo.following(),
                baseInfo.bio(),
                resolveCreditLabel(sellerStats),
                resolveCreditLabel(buyerStats),
                followed);
    }

    public List<OtherProfileItemDto> getOtherItems(Long viewerUserId, Long userId, String username, String status,
            String sort) {
        Long targetUserId = resolveTargetUserId(userId, username);
        return profileRepository.findOtherItems(targetUserId, normalizeOtherStatus(status), normalizeOtherSort(sort));
    }

    public OtherProfileReviewSummaryDto getOtherReviews(Long viewerUserId, Long userId, String username,
            String rating) {
        Long targetUserId = resolveTargetUserId(userId, username);
        String normalizedRating = normalizeRating(rating);
        ProfileRepository.CreditStats sellerStats = profileRepository.findCreditStats(targetUserId, ROLE_SELLER);
        ProfileRepository.CreditStats buyerStats = profileRepository.findCreditStats(targetUserId, ROLE_BUYER);
        int totalGood = sellerStats.goodCount() + buyerStats.goodCount();
        int totalBad = sellerStats.badCount() + buyerStats.badCount();
        List<OtherProfileReviewItemDto> reviews = profileRepository.findReviews(targetUserId, normalizedRating)
                .stream()
                .map(this::toOtherReview)
                .collect(Collectors.toList());
        return new OtherProfileReviewSummaryDto(totalGood + totalBad, totalGood, totalBad, reviews);
    }

    @Transactional
    public void followOther(Long viewerUserId, Long userId, String username) {
        Long targetUserId = resolveTargetUserId(userId, username);
        if (viewerUserId.equals(targetUserId)) {
            throw new BusinessException("不能关注自己");
        }
        if (profileRepository.isFollowing(viewerUserId, targetUserId)) {
            return;
        }
        profileRepository.followUser(viewerUserId, targetUserId);
        messageNotificationService.notifyFollowed(targetUserId, viewerUserId);
    }

    @Transactional
    public void unfollowOther(Long viewerUserId, Long userId, String username) {
        Long targetUserId = resolveTargetUserId(userId, username);
        if (viewerUserId.equals(targetUserId)) {
            throw new BusinessException("不能取消关注自己");
        }
        profileRepository.unfollowUser(viewerUserId, targetUserId);
    }

    @Transactional
    public ProfileOverviewDto updateBasicInfo(Long userId, ProfileUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("资料参数无效");
        }
        String username = normalizeText(request.username());
        if (username.isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (username.length() > 20) {
            throw new BusinessException("用户名最多 20 个字符");
        }

        String password = normalizeText(request.password());
        String passwordHash = null;
        if (!password.isEmpty()) {
            if (password.length() < 6) {
                throw new BusinessException("新密码至少 6 位");
            }
            passwordHash = passwordEncoder.encode(password);
        }

        userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        LocalDateTime now = LocalDateTime.now();
        userRepository.updateBasicInfo(userId, username, passwordHash, now);

        String avatar = normalizeText(request.avatar());
        if (!avatar.isEmpty()) {
            profileRepository.ensureUserProfile(userId);
            profileRepository.updateAvatar(userId, avatar, now);
        }
        return getOverview(userId);
    }

    @Transactional
    public ProfileAvatarUploadDto uploadAvatar(Long userId, MultipartFile file) {
        profileRepository.ensureUserProfile(userId);
        String url = profileImageService.upload(file);
        profileRepository.updateAvatar(userId, url, LocalDateTime.now());
        return new ProfileAvatarUploadDto(url);
    }

    private String resolveCreditLabel(ProfileRepository.CreditStats stats) {
        return CreditLevelResolver.resolveLabel(stats.goodCount(), stats.badCount());
    }

    private String normalizeRating(String rating) {
        String normalized = normalizeText(rating).toLowerCase();
        if (normalized.isEmpty() || "all".equals(normalized)) {
            return "all";
        }
        if (!"good".equals(normalized) && !"bad".equals(normalized)) {
            throw new BusinessException("评价筛选参数无效");
        }
        return normalized;
    }

    private Long resolveTargetUserId(Long userId, String username) {
        if (userId != null && userId > 0) {
            if (profileRepository.userExists(userId)) {
                return userId;
            }
            throw new BusinessException("目标用户不存在");
        }
        String normalizedUsername = normalizeText(username);
        if (normalizedUsername.isEmpty()) {
            throw new BusinessException("缺少目标用户参数");
        }
        Long resolvedUserId = profileRepository.findUserIdByUsername(normalizedUsername);
        if (resolvedUserId == null) {
            throw new BusinessException("目标用户不存在");
        }
        return resolvedUserId;
    }

    private String normalizeOtherStatus(String status) {
        String normalized = normalizeText(status).toLowerCase();
        if (normalized.isEmpty()) {
            return "all";
        }
        if (!"all".equals(normalized) && !"onsale".equals(normalized) && !"sold".equals(normalized)) {
            throw new BusinessException("宝贝筛选参数无效");
        }
        return normalized;
    }

    private String normalizeOtherSort(String sort) {
        String normalized = normalizeText(sort).toLowerCase();
        if (normalized.isEmpty()) {
            return "price-desc";
        }
        if (!"price-desc".equals(normalized) && !"price-asc".equals(normalized)) {
            throw new BusinessException("排序参数无效");
        }
        return normalized;
    }

    private OtherProfileReviewItemDto toOtherReview(ProfileReviewItemDto item) {
        return new OtherProfileReviewItemDto(
                item.id(),
                item.user(),
                item.avatar(),
                item.rating(),
                item.content(),
                item.time(),
                "bad".equalsIgnoreCase(item.rating()) ? "差评" : "好评");
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
