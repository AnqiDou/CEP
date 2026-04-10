package cep_backend.service;
import cep_backend.common.exception.BusinessException;
import cep_backend.mapper.UserRepository;
import cep_backend.service.MessageNotificationService;
import cep_backend.dto.ProfileOverviewDto;
import cep_backend.dto.ProfileAvatarUploadDto;
import cep_backend.dto.ProfileFollowUserDto;
import cep_backend.dto.OtherProfileItemDto;
import cep_backend.dto.OtherProfileOverviewDto;
import cep_backend.dto.OtherProfileReviewItemDto;
import cep_backend.dto.OtherProfileReviewSummaryDto;
import cep_backend.dto.ProfilePendingTradeDto;
import cep_backend.dto.ProfileReviewItemDto;
import cep_backend.dto.ProfileReviewSummaryDto;
import cep_backend.dto.ProfileTradeContactDto;
import cep_backend.dto.ProfileTradeItemDto;
import cep_backend.dto.ProfileUpdateRequest;
import cep_backend.mapper.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
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
        BigDecimal sellerCreditScore = profileRepository.findUserCreditScore(userId, ROLE_SELLER);
        BigDecimal buyerCreditScore = profileRepository.findUserCreditScore(userId, ROLE_BUYER);
        return new ProfileOverviewDto(
                baseInfo.avatar(),
                baseInfo.username(),
                baseInfo.name(),
                baseInfo.phone(),
                baseInfo.address(),
                baseInfo.fans(),
                baseInfo.following(),
                sellerCreditScore,
                buyerCreditScore,
                resolveCreditLabel(sellerCreditScore),
                resolveCreditLabel(buyerCreditScore));
    }

    public ProfileReviewSummaryDto getReviews(Long userId, String rating) {
        profileRepository.ensureUserProfile(userId);
        String normalized = normalizeRating(rating);
        ProfileRepository.CreditStats sellerStats = profileRepository.findCreditStats(userId, ROLE_SELLER);
        ProfileRepository.CreditStats buyerStats = profileRepository.findCreditStats(userId, ROLE_BUYER);
        int totalGood = sellerStats.goodCount() + buyerStats.goodCount();
        int totalBad = sellerStats.badCount() + buyerStats.badCount();
        List<cep_backend.dto.ProfileReviewItemDto> reviews = profileRepository.findReviews(userId,
                normalized);
        return new ProfileReviewSummaryDto(totalGood + totalBad, totalGood, totalBad, reviews);
    }

    public List<ProfileTradeItemDto> getPublishedItems(Long userId) {
        return profileRepository.findPublishedItems(userId);
    }

    public List<ProfileTradeItemDto> getSoldItems(Long userId) {
        return getSoldItems(userId, "all");
    }

    public List<ProfileTradeItemDto> getSoldItems(Long userId, String status) {
        return profileRepository.findSoldItems(userId, normalizeTradeOrderStatus(status));
    }

    public List<ProfileTradeItemDto> getBoughtItems(Long userId) {
        return getBoughtItems(userId, "all");
    }

    public List<ProfileTradeItemDto> getBoughtItems(Long userId, String status) {
        return profileRepository.findBoughtItems(userId, normalizeTradeOrderStatus(status));
    }

    public List<ProfileTradeItemDto> getFavoriteItems(Long userId) {
        return profileRepository.findFavoriteItems(userId);
    }

    public List<ProfileFollowUserDto> getFollowingUsers(Long userId) {
        return profileRepository.findFollowingUsers(userId);
    }

    public List<ProfileFollowUserDto> getFansUsers(Long userId) {
        return profileRepository.findFansUsers(userId);
    }

    public List<ProfilePendingTradeDto> getPendingPaymentTrades(Long userId) {
        return profileRepository.findPendingPaymentTrades(userId);
    }

    public ProfileTradeContactDto getSoldOrderBuyerContact(Long userId, Long orderId) {
        validateOrderId(orderId);
        ProfileRepository.TradeContactRecord record = profileRepository.findSoldOrderBuyerContact(userId, orderId);
        if (record == null || record.peerUserId() == null || record.peerUserId() <= 0) {
            throw new BusinessException("订单不存在或无操作权限");
        }
        return new ProfileTradeContactDto(
                record.orderId(),
                record.itemId(),
                record.peerUserId(),
                defaultPeerName(record.peerName()),
                defaultItemTitle(record.itemTitle()));
    }

    public ProfileTradeContactDto getBoughtOrderSellerContact(Long userId, Long orderId) {
        validateOrderId(orderId);
        ProfileRepository.TradeContactRecord record = profileRepository.findBoughtOrderSellerContact(userId, orderId);
        if (record == null || record.peerUserId() == null || record.peerUserId() <= 0) {
            throw new BusinessException("订单不存在或无操作权限");
        }
        return new ProfileTradeContactDto(
                record.orderId(),
                record.itemId(),
                record.peerUserId(),
                defaultPeerName(record.peerName()),
                defaultItemTitle(record.itemTitle()));
    }

    public ProfileTradeItemDto rebuyItem(Long userId, Long orderId) {
        validateOrderId(orderId);
        ProfileRepository.TradeContactRecord record = profileRepository.findBoughtOrderSellerContact(userId, orderId);
        if (record == null) {
            throw new BusinessException("订单不存在或无操作权限");
        }
        Long itemId = record.itemId();
        Long sellerUserId = record.peerUserId();
        if (itemId == null || itemId <= 0 || sellerUserId == null || sellerUserId <= 0) {
            throw new BusinessException("订单关联物品信息无效");
        }
        if (!profileRepository.isPublishedItemOwnedBy(itemId, sellerUserId)) {
            throw new BusinessException("当前商品已下架，暂不支持再次购买");
        }

        return new ProfileTradeItemDto(
                orderId,
                null,
                itemId,
                defaultItemTitle(record.itemTitle()),
                null,
                "",
                "",
                "PUBLISHED",
                null,
                null,
                null,
                null);
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
        BigDecimal sellerCreditScore = profileRepository.findUserCreditScore(targetUserId, ROLE_SELLER);
        BigDecimal buyerCreditScore = profileRepository.findUserCreditScore(targetUserId, ROLE_BUYER);
        return new OtherProfileOverviewDto(
                targetUserId,
                baseInfo.avatar(),
                baseInfo.username(),
                baseInfo.fans(),
                baseInfo.following(),
                sellerCreditScore,
                buyerCreditScore,
                resolveCreditLabel(sellerCreditScore),
                resolveCreditLabel(buyerCreditScore),
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
        String name = normalizeText(request.name());
        String phone = normalizeText(request.phone());
        String address = normalizeText(request.address());
        if (name.isEmpty()) {
            throw new BusinessException("姓名不能为空");
        }
        if (phone.isEmpty()) {
            throw new BusinessException("联系电话不能为空");
        }
        if (address.isEmpty()) {
            throw new BusinessException("收货地址不能为空");
        }

        userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        LocalDateTime now = LocalDateTime.now();
        userRepository.updateBasicInfo(userId, username, now);
        profileRepository.ensureUserProfile(userId);
        profileRepository.updateContactInfo(userId, name, phone, address, now);

        String avatar = normalizeText(request.avatar());
        if (!avatar.isEmpty()) {
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

    private String resolveCreditLabel(BigDecimal score) {
        return CreditLevelResolver.resolveLabel(score);
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

    private String normalizeTradeOrderStatus(String status) {
        String normalized = normalizeText(status).toLowerCase();
        if (normalized.isEmpty()) {
            return "all";
        }
        if (!"all".equals(normalized)
                && !"pending-payment".equals(normalized)
                && !"pending-confirmation".equals(normalized)
                && !"completed".equals(normalized)
                && !"cancelled".equals(normalized)) {
            throw new BusinessException("交易状态筛选参数无效");
        }
        return normalized;
    }

    private OtherProfileReviewItemDto toOtherReview(ProfileReviewItemDto item) {
        return new OtherProfileReviewItemDto(
                item.id(),
                item.user(),
                item.avatar(),
                item.identity(),
                item.rating(),
                item.content(),
                item.time(),
                "bad".equalsIgnoreCase(item.rating()) ? "差评" : "好评");
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单参数无效");
        }
    }

    private String defaultPeerName(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? "校园用户" : normalized;
    }

    private String defaultItemTitle(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? "未命名物品" : normalized;
    }
}
