-- sample for create blog
CREATE TABLE `blog`
(
    `id`          int(11) NOT NULL AUTO_INCREMENT,
    `title`       varchar(255) DEFAULT NULL,
    `content`     varchar(255) DEFAULT NULL,
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;
