DROP TABLE IF EXISTS `character_quests`;
CREATE TABLE IF NOT EXISTS `character_quests` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `name` VARCHAR(60) NOT NULL DEFAULT '',
  `var`  VARCHAR(20) NOT NULL DEFAULT '',
  `value` VARCHAR(255) ,
  `class_index` int(1) NOT NULL DEFAULT '0',
  
  PRIMARY KEY (`charId`,`name`,`var`,`class_index`),
  FOREIGN KEY FK_CHARACTER_QUEST (`charId`) REFERENCES characters (`charId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4;