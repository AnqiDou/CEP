-- Auto generated: SQL Server => MySQL
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS `CEP`;
CREATE DATABASE `CEP` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `CEP`;

DROP TABLE IF EXISTS `admin_notices`;
CREATE TABLE `admin_notices` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `admin_order_abnormal_records`;
CREATE TABLE `admin_order_abnormal_records` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `handled` TINYINT(1) NOT NULL DEFAULT 0,
  `handled_note` VARCHAR(200) NULL,
  `handled_by_user_id` BIGINT NULL,
  `handled_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_admin_order_abnormal_records_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `admin_support_conversations`;
CREATE TABLE `admin_support_conversations` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `preview` VARCHAR(200) NOT NULL DEFAULT '',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `admin_support_messages`;
CREATE TABLE `admin_support_messages` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `conversation_id` BIGINT NOT NULL,
  `sender_type` VARCHAR(20) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `auth_sessions`;
CREATE TABLE `auth_sessions` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `refresh_token_hash` VARCHAR(128) NOT NULL,
  `access_token_hash` VARCHAR(128) NOT NULL,
  `refresh_expires_at` DATETIME(6) NOT NULL,
  `access_expires_at` DATETIME(6) NOT NULL,
  `revoked` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__auth_ses__04738B0507C11C8B` (`access_token_hash`),
  UNIQUE KEY `UQ__auth_ses__63546C903DD97B98` (`refresh_token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `email_verification_codes`;
CREATE TABLE `email_verification_codes` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `purpose` VARCHAR(50) NOT NULL,
  `code` VARCHAR(10) NOT NULL,
  `used` TINYINT(1) NOT NULL DEFAULT 0,
  `expires_at` DATETIME(6) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `item_categories`;
CREATE TABLE `item_categories` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `code` VARCHAR(30) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(200) NOT NULL,
  `tags` VARCHAR(300) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__item_cat__357D4CF97361E710` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `item_details`;
CREATE TABLE `item_details` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `publisher_user_id` BIGINT NULL,
  `purchase_date` DATE NULL,
  `usage_duration` VARCHAR(50) NULL,
  `item_condition` VARCHAR(50) NULL,
  `accessories` VARCHAR(200) NULL,
  `detail_note` VARCHAR(300) NULL,
  `trade_location` VARCHAR(80) NULL,
  `original_price` DECIMAL(10,2) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__item_det__52020FDC522A5824` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `item_photos`;
CREATE TABLE `item_photos` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `photo_url` VARCHAR(500) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `category_id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `description` VARCHAR(500) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `badge` VARCHAR(20) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  `view_count` INT NOT NULL DEFAULT 0,
  `favorite_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `publisher_user_id` BIGINT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `message_conversations`;
CREATE TABLE `message_conversations` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `buyer_user_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NOT NULL,
  `last_message` VARCHAR(1000) NULL,
  `last_message_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  `unread_buyer` INT NOT NULL DEFAULT 0,
  `unread_seller` INT NOT NULL DEFAULT 0,
  `last_message_at` DATETIME(6) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_message_conversations_item_pair` (`item_id`, `buyer_user_id`, `seller_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `message_records`;
CREATE TABLE `message_records` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `conversation_id` BIGINT NOT NULL,
  `sender_user_id` BIGINT NOT NULL,
  `message_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  `text_content` VARCHAR(2000) NULL,
  `image_url` VARCHAR(500) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `read_at` DATETIME(6) NULL,
  `biz_type` VARCHAR(40) NULL,
  `biz_id` BIGINT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `search_keywords`;
CREATE TABLE `search_keywords` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `keyword` VARCHAR(100) NOT NULL,
  `search_count` BIGINT NOT NULL DEFAULT 0,
  `last_searched_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__search_k__3697F5A218717CD5` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `system_notices`;
CREATE TABLE `system_notices` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `created_by_user_id` BIGINT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `trade_orders`;
CREATE TABLE `trade_orders` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `order_no` VARCHAR(40) NOT NULL,
  `item_id` BIGINT NOT NULL,
  `item_title` VARCHAR(120) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `cover_photo_url` VARCHAR(500) NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(30) NOT NULL,
  `receiver_address` VARCHAR(200) NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
  `paid_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `buyer_user_id` BIGINT NULL,
  `seller_user_id` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__trade_or__465C81B87D03678E` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `trade_payment_orders`;
CREATE TABLE `trade_payment_orders` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `order_no` VARCHAR(40) NOT NULL,
  `item_id` BIGINT NOT NULL,
  `buyer_user_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NULL,
  `item_title` VARCHAR(120) NOT NULL,
  `seller_name` VARCHAR(50) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `channel` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `payment_url` VARCHAR(300) NOT NULL,
  `expire_at` DATETIME(6) NOT NULL,
  `paid_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__trade_pa__465C81B8832F6220` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `trade_review_tasks`;
CREATE TABLE `trade_review_tasks` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `reviewer_user_id` BIGINT NOT NULL,
  `target_user_id` BIGINT NOT NULL,
  `target_role` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `reviewed_at` DATETIME(6) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_trade_review_tasks` (`order_id`, `reviewer_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `user_credit_reviews`;
CREATE TABLE `user_credit_reviews` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `order_id` BIGINT NULL,
  `rater_user_id` BIGINT NOT NULL,
  `target_user_id` BIGINT NOT NULL,
  `target_role` VARCHAR(20) NOT NULL,
  `rating` VARCHAR(10) NOT NULL,
  `content` VARCHAR(300) NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `user_favorites`;
CREATE TABLE `user_favorites` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_favorites` (`user_id`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `user_follows`;
CREATE TABLE `user_follows` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `target_user_id` BIGINT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_follows` (`user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `user_profiles`;
CREATE TABLE `user_profiles` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `college` VARCHAR(80) NULL,
  `credit_score` DECIMAL(3,1) NULL,
  `note` VARCHAR(200) NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `avatar_url` VARCHAR(500) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__user_pro__B9BE370E658B0B59` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `username` VARCHAR(50) NULL,
  `password_hash` VARCHAR(100) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `last_login_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UQ__users__AB6E6164AD7CE044` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


INSERT INTO `admin_support_conversations` (`id`, `title`, `preview`, `status`, `created_at`, `updated_at`) VALUES
(1, 'Order dispute: item mismatch', 'User reported item does not match description', 'OPEN', '2026-03-29 19:42:32.030000', '2026-03-29 19:42:32.030000'),
(2, 'Report: prohibited listing', 'User reported prohibited contact info in detail page', 'OPEN', '2026-03-29 19:42:32.030000', '2026-03-29 19:42:32.030000');

INSERT INTO `admin_support_messages` (`id`, `conversation_id`, `sender_type`, `content`, `created_at`) VALUES
(1, 1, 'USER', 'The item model is different from the page description.', '2026-03-29 19:42:32.036666'),
(2, 1, 'USER', 'Please ask platform support to intervene.', '2026-03-29 19:42:32.036666'),
(3, 2, 'USER', 'The listing detail includes prohibited external contact info.', '2026-03-29 19:42:32.036666');

INSERT INTO `auth_sessions` (`id`, `user_id`, `refresh_token_hash`, `access_token_hash`, `refresh_expires_at`, `access_expires_at`, `revoked`, `created_at`, `updated_at`) VALUES
(1, 1, '0343c79786983495a829c371365722d72da9ed2764a6b6f7432c90ae39ff7695', '6b55c2fb89dca1e75b53478403dccac30590f1e7e6b16349233d5ca5dcaadc6f', '2026-03-31 14:10:18.194886', '2026-03-24 14:25:18.194886', 1, '2026-03-24 13:42:16.446016', '2026-03-24 14:20:44.154706'),
(2, 1, 'f680ffd592466bbe4c6fb98db31e2399ca37e1309b918f8117846c234ae17bbf', 'b6a6a48bd0c786f46106937a442e01e059d230a7b1acd3bebf83f3fb9e0dcd5c', '2026-03-31 14:39:23.926910', '2026-03-24 14:54:23.926910', 1, '2026-03-24 14:39:23.926910', '2026-03-24 14:49:13.814051'),
(3, 1, '18c27a74975b9d3a79bbca20ca149d44b80515705b915a32689568e1b9058772', 'aa37d89d2e7e8163c93139c1e01b872154e75a32bee805726d363b1fd4549519', '2026-03-31 15:11:13.834633', '2026-03-24 15:26:13.834633', 0, '2026-03-24 14:57:13.440234', '2026-03-24 15:11:13.834633'),
(4, 1, '4e812163333bd1e894c2724cefefe0e2957935a3bde5061abd1a930816c02cf6', 'd9934eaa06a7a16b4ee3b81f0f38d3ce0ecca94878200f96b168d417c7011048', '2026-03-31 16:50:50.742138', '2026-03-24 17:05:50.742138', 1, '2026-03-24 16:08:48.024350', '2026-03-24 16:55:29.505325'),
(5, 1, '8f9d4fc9ea487d5f9430528a3a63558b3412fa2a16450ffd2217a723fe379d06', '65ce23b84601c53d3f79844644c4a6c2065d8ec0b3597e7431726c17b8726a50', '2026-03-31 16:55:45.824284', '2026-03-24 17:10:45.824284', 0, '2026-03-24 16:55:45.824284', '2026-03-24 16:55:45.824284'),
(6, 1, '37f9eac3ec579ea2239709eb232643f8c6f54d835ce37319535eda785d0c65fc', 'be53f9fa66b5d8abecb6410aad62e76852cf7cf456e047bd79e43d09e11c722c', '2026-03-31 18:59:00.487086', '2026-03-24 19:14:00.487086', 0, '2026-03-24 17:06:52.481186', '2026-03-24 18:59:00.487086'),
(7, 1, '3b18b3a7a7893a086929f697e93607052ac4a1882b4e26d367d37d4ab0f054ba', '5227378e1f0d6a34ca3b1a509cfb11b0fc0d50ae6252b94c57ce31e8e77af129', '2026-04-01 14:21:08.338823', '2026-03-25 14:36:08.338823', 0, '2026-03-25 12:43:01.189347', '2026-03-25 14:21:08.338823'),
(8, 1, '7b509cd2bec150fcf39774a3011190ee96737419683e10c18b2b2b177675e3a3', 'cd280f3b90c1c7a33bec4c6f16d09d61f045a8f82315caaf54c677634a29e9b6', '2026-04-01 15:30:57.666140', '2026-03-25 15:45:57.666140', 1, '2026-03-25 15:16:56.041450', '2026-03-25 15:31:10.062851'),
(9, 1, '535abf8768791848948ce04c4aa36b5174463da04e92b2fed34818a43cf8f009', 'eadb54f9d03980f2cfe145ef3b4658a848723c28d714603bcb8105d7f27670b2', '2026-04-01 15:31:21.739118', '2026-03-25 15:46:21.739118', 1, '2026-03-25 15:31:21.739118', '2026-03-25 15:37:53.516547'),
(10, 1, '26ada3f1f35a018fa1c564725e5abeba2035f1a5c7e555834dd75d9fe832230e', '5722329d473b9af360e69845dda9e414f20bf8f11d0790214517c4e6e523beae', '2026-04-01 15:38:04.845671', '2026-03-25 15:53:04.845671', 1, '2026-03-25 15:38:04.845671', '2026-03-25 15:38:22.579883'),
(11, 1, '8cd15107e5e09b8f6f8149b03383db9daea804bb26b16ea219c1d6d925873d14', '686a2585c4b43db04938e0775ced38fe97138851eefd55730dc83dc769317635', '2026-04-01 15:38:32.798043', '2026-03-25 15:53:32.798043', 1, '2026-03-25 15:38:32.798043', '2026-03-25 15:39:00.591409'),
(12, 1, 'fcbdaa94bc5d1544c3641187db9ed5036c7c94f3d3f42bd756c9da24b0ffd3fc', '3a03a8e1fcf94f690790e3f45ae366f7f43abd8fc2032b47ae4f34890001bf07', '2026-04-01 15:39:09.333653', '2026-03-25 15:54:09.333653', 1, '2026-03-25 15:39:09.333653', '2026-03-25 15:41:43.129405'),
(13, 1, '2df492a9e632257420e6c53c534c13ab55c832d45d2475837c5909ea902bd71d', 'bd5e056f74d2838f3307be92047d861d5e105c5fac4c207b1a020c37d9259a7d', '2026-04-01 17:33:59.365531', '2026-03-25 17:48:59.365531', 1, '2026-03-25 15:41:51.126594', '2026-03-25 17:43:35.593284'),
(14, 1, '5689ffbb07e10fb57a958a07e39f63b0fc22730f22125304b1c9ddf6a6388793', '155c5f97d9cb16a0d64bb88c8fa762d291df9eb2665199a3d61f7925a27fa4ea', '2026-04-02 17:55:38.453110', '2026-03-26 18:10:38.453110', 0, '2026-03-25 17:43:48.127964', '2026-03-26 17:55:38.453110'),
(15, 1, '381f1fdf7f2e1f28f340bb3f9ed3c400b5c7a8b2c16d669689dabf20187963ca', '4a156224d9323e00dd78120fbe2df75857b6b496bb400ffd04c26d6967067e04', '2026-04-03 13:24:54.593850', '2026-03-27 13:39:54.593850', 1, '2026-03-26 18:01:07.874825', '2026-03-27 13:26:35.274376'),
(17, 1, '64b4aca5920b86d9b35cc4eda11bd27d5270a0495aad73b963e6f14d9a9887d8', 'a3e0633bb54ca2ff949f3cbd444613a92167283682d93a83a101011b3febb0a0', '2026-04-03 13:46:17.507870', '2026-03-27 14:01:17.507870', 1, '2026-03-27 13:46:17.507870', '2026-03-27 13:49:21.030669'),
(18, 1, '7b4eb5d56b223250711511b0cbc045d12fd87bae3ad4a8abd4c3558c4073c08a', '65c07d20bd791199b61c613c8279d0600150f935ba6777ab7ff0f9bf496b1ff9', '2026-04-04 18:18:48.802557', '2026-03-28 18:33:48.802557', 1, '2026-03-27 13:57:28.465574', '2026-03-28 18:20:23.363350'),
(19, 2, '40e29c88d67e28e0c19255110cd7412fd1d2e6224bb5f8752395afb927ac1fe1', '06e2d251c100bf348b374ab010719e120de0c02b5f7076bbae919bc46a1899e8', '2026-04-04 18:21:07.510102', '2026-03-28 18:36:07.510102', 1, '2026-03-28 18:21:07.510102', '2026-03-28 18:21:46.872185'),
(20, 1, '9f3c67b9ccf97314b8181ad33df1573f82df4b142fb3ef0ce31d0081254e9b78', '758941d4a1393542d7c8d4b1ee125d759ab8b4c40ee6cd70698cd6f00bd7d009', '2026-04-05 14:36:04.598574', '2026-03-29 14:51:04.598574', 1, '2026-03-28 18:21:56.452976', '2026-03-29 14:38:04.740280'),
(21, 2, '03723e13367ae974dd5e4632c974c520a4470291a2a3f46db0b328b265be3de4', '2fbcfa5a660a6a0ab9dd10aaa9913bce9f5eedbefd3cc6c7d2f6b109668aba30', '2026-04-05 15:38:26.961241', '2026-03-29 15:53:26.961241', 1, '2026-03-29 14:38:54.052183', '2026-03-29 15:40:31.073696'),
(22, 2, 'bbe3d63a2d072986b8af8bdaf347e5c6db1c27ca0735f5853f7c73c19210ef1f', '821f99cd59234f95e230720a1bbbfa87a2d2d2f0cf8002f1cc0ffbdf39c5ea1a', '2026-04-05 15:40:41.193003', '2026-03-29 15:55:41.193003', 1, '2026-03-29 15:40:41.193003', '2026-03-29 15:50:02.440849'),
(23, 2, 'dad3fd745b7b9a64d84348d5c0405de46d3ab3c44bd593c85eea19e4dae58c4d', '958e164667c3895703c44ec3ffefea7cc16755d45174c2f2f717c82e93ae3c39', '2026-04-05 15:50:13.487195', '2026-03-29 16:05:13.487195', 1, '2026-03-29 15:50:13.487195', '2026-03-29 15:50:18.930711'),
(24, 1, '0e22a814fad41800cbb54e35e4e5e7db4dbb94b30b715865e3c4261b58988917', '9421994ac73884da9fb91e44e0f87a1c2b76c5c2c8ce3a9a8c4983f5a0000935', '2026-04-05 16:32:29.492348', '2026-03-29 16:47:29.492348', 1, '2026-03-29 15:50:27.358950', '2026-03-29 16:32:51.169020'),
(25, 2, '6489c7ea711528cea560fea25182bf9aac7e23226ee583a4a412667f0f759f66', '518c515533cb9a38b12683d8bdfbf90a5e9800df43e4f42d0f6d46958b0dc71d', '2026-04-05 19:12:15.709180', '2026-03-29 19:27:15.709180', 1, '2026-03-29 16:32:59.666666', '2026-03-29 19:20:20.822398'),
(26, 2, '05331db7d1935a65cbd3f27ad35ca0a2e7d3fe1d66da857dd51a8aae856b0d3f', '0ac48674bd6a357a5cd55711231711b6c594b1408a8c634bf75c5f11d3182885', '2026-04-06 12:41:37.341245', '2026-03-30 12:56:37.341245', 1, '2026-03-29 19:36:03.116044', '2026-03-30 12:50:01.505277'),
(27, 1, 'eed8c7e7a0f5bc51f1e118d8e36b1a07e597a12974070102cf2a5e79e6831a9e', 'a028fc1b4b9ecb2ea0f6fe8b1f35d3c7e4f0f2cd4f8310109135edcca1f0c331', '2026-04-07 17:30:21.490563', '2026-03-31 17:45:21.490563', 0, '2026-03-30 14:22:39.716395', '2026-03-31 17:30:21.490563'),
(28, 2, '4175d5e2d616f2a7bbba07233973e32949226d6fe6b8ae6ed8549b74928f1312', 'f10c27488be429a82ac3077da03ebe1948576eeac3de9c2d18fe244fbec1588f', '2026-04-07 16:31:35.489467', '2026-03-31 16:46:35.489467', 0, '2026-03-31 14:53:28.478509', '2026-03-31 16:31:35.489467');

INSERT INTO `email_verification_codes` (`id`, `email`, `purpose`, `code`, `used`, `expires_at`, `created_at`) VALUES
(1, '3299166215@qq.com', 'REGISTER', '549731', 1, '2026-03-27 13:54:42.008885', '2026-03-27 13:44:42.008885'),
(2, '3299166215@qq.com', 'RESET_PASSWORD', '496922', 1, '2026-03-29 14:48:18.918355', '2026-03-29 14:38:18.918355');

INSERT INTO `item_categories` (`id`, `code`, `name`, `description`, `tags`, `sort_order`, `created_at`, `updated_at`) VALUES
(1, 'digital', '鏁扮爜浜у搧', '鎵嬫満銆佺數鑴戙€佸钩鏉裤€佽€虫満銆佸厖鐢靛櫒绛?, '鎵嬫満,鐢佃剳,骞虫澘,鑰虫満,鍏呯數鍣?, 1, '2026-03-25 17:32:07.846666', '2026-03-25 17:32:07.846666'),
(2, 'book', '鍥句功鏁欐潗', '璇炬湰銆佽€冪爺鑰冨叕璧勬枡銆佸皬璇淬€佷笓涓氫功', '璇炬湰,鑰冪爺,鑰冨叕,灏忚,涓撲笟涔?, 2, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000'),
(3, 'clothes', '鏈嶉グ闉嬪寘', '琛ｆ湇銆侀瀷瀛愩€佸寘鍖呫€侀厤楗?, '琛ｆ湇,闉嬪瓙,鍖呭寘,閰嶉グ', 3, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000'),
(4, 'beauty', '缇庡鎶よ偆', '鍖栧鍝併€佹姢鑲ゅ搧銆侀姘?, '鍖栧鍝?鎶よ偆鍝?棣欐按', 4, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000'),
(5, 'sports', '杩愬姩鍣ㄦ潗', '绡悆銆佺窘姣涚悆鎷嶃€佺憸浼藉灚銆佽嚜琛岃溅', '绡悆,缇芥瘺鐞冩媿,鐟滀冀鍨?鑷杞?, 5, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000'),
(6, 'daily', '鐢熸椿鐢ㄥ搧', '鏀剁撼銆佸皬瀹剁數銆侀攨纰楃摙鐩嗐€佸瘽瀹ょ敤鍝?, '鏀剁撼,灏忓鐢?閿呯鐡㈢泦,瀵濆鐢ㄥ搧', 6, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000'),
(7, 'stationery', '鏂囧叿鍔炲叕', '绗斻€佹湰銆佽绠楀櫒銆佹枃浠跺す绛?, '绗?鏈?璁＄畻鍣?鏂囦欢澶?, 7, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000'),
(8, 'other', '鍏朵粬', '鍏朵粬鍒嗙被鍟嗗搧', '鍏朵粬', 8, '2026-03-25 17:32:07.850000', '2026-03-25 17:32:07.850000');

INSERT INTO `item_details` (`id`, `item_id`, `publisher_user_id`, `purchase_date`, `usage_duration`, `item_condition`, `accessories`, `detail_note`, `trade_location`, `original_price`, `created_at`, `updated_at`) VALUES
(1, 7, 1, '2026-03-28 00:00:00.000000', '', '', '', '', '', NULL, '2026-03-28 18:38:19.060000', '2026-03-28 18:38:19.060000'),
(2, 8, 1, '2026-03-28 00:00:00.000000', '', '', '', '', '', NULL, '2026-03-28 18:38:19.060000', '2026-03-28 18:38:19.060000'),
(3, 9, 1, '2026-03-28 00:00:00.000000', '', '', '', '', '', NULL, '2026-03-28 18:38:19.060000', '2026-03-28 18:38:19.060000'),
(4, 10, 1, '2026-03-28 00:00:00.000000', '', '', '', '', '', NULL, '2026-03-28 18:38:19.060000', '2026-03-28 18:38:19.060000'),
(5, 11, 1, '2026-03-28 00:00:00.000000', '', '', '', '', '', NULL, '2026-03-28 18:38:19.060000', '2026-03-28 18:38:19.060000');

INSERT INTO `item_photos` (`id`, `item_id`, `photo_url`, `sort_order`, `created_at`) VALUES
(1, 8, 'https://cep-project-1416369898.cos.ap-beijing.myqcloud.com/publish-images/2026-03-26/c3422f83-0091-4375-a537-ea8cd5803d1e.jpg', 1, '2026-03-26 21:14:07.142872'),
(2, 9, 'https://cep-project-1416369898.cos.ap-beijing.myqcloud.com/publish-images/2026-03-26/891ff9d9-22e8-469e-aff7-55d0e4f3bb5b.png', 1, '2026-03-26 21:37:07.652686'),
(3, 10, 'https://cep-project-1416369898.cos.ap-beijing.myqcloud.com/publish-images/2026-03-26/5615e9ae-7a9d-4968-bd17-35988ad41e3f.png', 1, '2026-03-26 21:50:04.715434'),
(4, 11, 'https://cep-project-1416369898.cos.ap-beijing.myqcloud.com/publish-images/2026-03-27/ce87ea6d-979b-45cd-ba4c-ed2bde379e76.png', 1, '2026-03-27 13:26:18.316948');

INSERT INTO `items` (`id`, `category_id`, `title`, `description`, `price`, `badge`, `status`, `view_count`, `favorite_count`, `created_at`, `updated_at`, `publisher_user_id`) VALUES
(7, 8, 'tset1', '', 1.00, NULL, 'PUBLISHED', 0, 0, '2026-03-26 21:13:49.451331', '2026-03-26 21:13:49.451331', 1),
(8, 8, 'test2', '', 2.00, NULL, 'PUBLISHED', 1, 0, '2026-03-26 21:14:07.142872', '2026-03-30 15:48:47.233333', 1),
(9, 8, 'test3', '', 3.00, NULL, 'PUBLISHED', 0, 0, '2026-03-26 21:37:07.652686', '2026-03-26 21:37:07.652686', 1),
(10, 8, 't4', '', 4.00, NULL, 'PUBLISHED', 1, 0, '2026-03-26 21:50:04.715434', '2026-03-30 16:06:35.073333', 1),
(11, 8, '5', '', 5.00, NULL, 'DELETED', 25, 0, '2026-03-27 13:26:18.316948', '2026-03-29 16:32:34.980340', 1);



INSERT INTO `search_keywords` (`id`, `keyword`, `search_count`, `last_searched_at`, `created_at`, `updated_at`) VALUES
(1, '1', 1, '2026-03-28 14:46:12.543635', '2026-03-28 14:46:12.543635', '2026-03-28 14:46:12.543635'),
(2, '2', 1, '2026-03-28 14:46:16.347708', '2026-03-28 14:46:16.347708', '2026-03-28 14:46:16.347708'),
(3, 't', 1, '2026-03-28 14:46:24.971843', '2026-03-28 14:46:24.971843', '2026-03-28 14:46:24.971843');


INSERT INTO `trade_orders` (`id`, `order_no`, `item_id`, `item_title`, `amount`, `cover_photo_url`, `receiver_name`, `receiver_phone`, `receiver_address`, `status`, `paid_at`, `created_at`, `updated_at`, `buyer_user_id`, `seller_user_id`) VALUES
(1, 'CEP20260328162524239699', 11, '5', 5.00, 'https://cep-project-1416369898.cos.ap-beijing.myqcloud.com/publish-images/2026-03-27/ce87ea6d-979b-45cd-ba4c-ed2bde379e76.png', '1', '1', '1', 'PAID', '2026-03-28 16:25:30.648175', '2026-03-28 16:25:24.873333', '2026-03-28 16:25:30.673333', NULL, NULL);

INSERT INTO `trade_payment_orders` (`id`, `order_no`, `item_id`, `buyer_user_id`, `seller_user_id`, `item_title`, `seller_name`, `amount`, `channel`, `status`, `payment_url`, `expire_at`, `paid_at`, `created_at`, `updated_at`) VALUES
(1, 'CEP20260328155824399383', 11, 1, NULL, '5', '鏍″洯鐢ㄦ埛', 5.00, 'WECHAT_H5', 'PENDING', 'https://pay-mock.cep.local/wechat/h5?orderNo=CEP20260328155824399383', '2026-03-28 16:13:24.592891', NULL, '2026-03-28 15:58:24.592891', '2026-03-28 15:58:24.592891'),
(2, 'CEP20260328155828718123', 11, 1, NULL, '5', '鏍″洯鐢ㄦ埛', 5.00, 'WECHAT_H5', 'PAID', 'https://pay-mock.cep.local/wechat/h5?orderNo=CEP20260328155828718123', '2026-03-28 16:13:28.730487', '2026-03-28 15:58:34.167421', '2026-03-28 15:58:28.730487', '2026-03-28 15:58:34.167421');





INSERT INTO `user_profiles` (`id`, `user_id`, `college`, `credit_score`, `note`, `created_at`, `updated_at`, `avatar_url`) VALUES
(1, 1, NULL, NULL, NULL, '2026-03-28 16:50:11.000000', '2026-03-28 16:57:02.952293', 'https://cep-project-1416369898.cos.ap-beijing.myqcloud.com/profile-avatars/2026-03-28/4e6116e3-f868-41c6-8289-1caedd8b082e.png'),
(2, 2, NULL, NULL, NULL, '2026-03-28 18:21:22.563333', '2026-03-28 18:21:22.563333', NULL);

INSERT INTO `users` (`id`, `email`, `username`, `password_hash`, `status`, `created_at`, `updated_at`, `last_login_at`) VALUES
(1, 'anqidou@outlook.com', '瀹夌惇', '$2a$10$Mq32x0gxLZs7KXRbuOqkRuLTKw4ArcmMRYQbt6ijlXOuylv2vg7Tm', 'ACTIVE', '2026-03-24 13:42:16.199417', '2026-03-30 14:22:39.716395', '2026-03-30 14:22:39.716395'),
(2, '3299166215@qq.com', '', '$2a$10$IYyvw7fLn6YrkhQkcUbJOuCNnCyMjFeLdOS3ypiK/fn1CuoLhni8G', 'ACTIVE', '2026-03-27 13:45:14.976395', '2026-03-31 14:53:28.478509', '2026-03-31 14:53:28.478509');

ALTER TABLE `admin_order_abnormal_records` ADD CONSTRAINT `fk_admin_order_abnormal_records_user` FOREIGN KEY (`handled_by_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `admin_order_abnormal_records` ADD CONSTRAINT `fk_admin_order_abnormal_records_order` FOREIGN KEY (`order_id`) REFERENCES `trade_orders` (`id`);
ALTER TABLE `admin_support_messages` ADD CONSTRAINT `fk_admin_support_messages_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `admin_support_conversations` (`id`);
ALTER TABLE `auth_sessions` ADD CONSTRAINT `fk_auth_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `item_details` ADD CONSTRAINT `fk_item_details_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`);
ALTER TABLE `item_details` ADD CONSTRAINT `fk_item_details_publisher` FOREIGN KEY (`publisher_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `items` ADD CONSTRAINT `fk_items_publisher` FOREIGN KEY (`publisher_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `items` ADD CONSTRAINT `fk_items_category` FOREIGN KEY (`category_id`) REFERENCES `item_categories` (`id`);
ALTER TABLE `message_conversations` ADD CONSTRAINT `fk_message_conversations_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`);
ALTER TABLE `message_conversations` ADD CONSTRAINT `fk_message_conversations_buyer` FOREIGN KEY (`buyer_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `message_conversations` ADD CONSTRAINT `fk_message_conversations_seller` FOREIGN KEY (`seller_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `message_records` ADD CONSTRAINT `fk_message_records_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `message_records` ADD CONSTRAINT `fk_message_records_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `message_conversations` (`id`);
ALTER TABLE `system_notices` ADD CONSTRAINT `fk_system_notices_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `trade_orders` ADD CONSTRAINT `fk_trade_orders_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`);
ALTER TABLE `trade_payment_orders` ADD CONSTRAINT `fk_trade_payment_orders_seller` FOREIGN KEY (`seller_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `trade_payment_orders` ADD CONSTRAINT `fk_trade_payment_orders_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`);
ALTER TABLE `trade_payment_orders` ADD CONSTRAINT `fk_trade_payment_orders_buyer` FOREIGN KEY (`buyer_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `trade_review_tasks` ADD CONSTRAINT `fk_trade_review_tasks_reviewer` FOREIGN KEY (`reviewer_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `trade_review_tasks` ADD CONSTRAINT `fk_trade_review_tasks_target` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `trade_review_tasks` ADD CONSTRAINT `fk_trade_review_tasks_order` FOREIGN KEY (`order_id`) REFERENCES `trade_orders` (`id`);
ALTER TABLE `user_credit_reviews` ADD CONSTRAINT `fk_user_credit_reviews_order` FOREIGN KEY (`order_id`) REFERENCES `trade_orders` (`id`);
ALTER TABLE `user_credit_reviews` ADD CONSTRAINT `fk_user_credit_reviews_rater` FOREIGN KEY (`rater_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_credit_reviews` ADD CONSTRAINT `fk_user_credit_reviews_target` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_favorites` ADD CONSTRAINT `fk_user_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_favorites` ADD CONSTRAINT `fk_user_favorites_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`);
ALTER TABLE `user_follows` ADD CONSTRAINT `fk_user_follows_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_follows` ADD CONSTRAINT `fk_user_follows_target` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_profiles` ADD CONSTRAINT `fk_user_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
SET FOREIGN_KEY_CHECKS = 1;


