USE attendance_system;

ALTER TABLE attendance_system.biz_attendance_record
MODIFY COLUMN class_photo_url TEXT DEFAULT NULL;
