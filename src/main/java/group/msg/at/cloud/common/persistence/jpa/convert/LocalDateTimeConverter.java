package group.msg.at.cloud.common.persistence.jpa.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * {@code JPA Attribute Converter} to have support for {@code LocalDateTime}
 * attributes and PostgreSQL {@code TIMESTAMPTZ} columns.
 *
 * @author Michael Theis (michael.theis@msg.group)
 * @version 1.0
 * @since release 1.0.0
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime attribute) {
        Timestamp result = null;
        if (attribute != null) {
            result = Timestamp.valueOf(attribute);
        }
        return result;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp dbData) {
        LocalDateTime result = null;
        if (dbData != null) {
            result = dbData.toLocalDateTime();
        }
        return result;
    }
}
