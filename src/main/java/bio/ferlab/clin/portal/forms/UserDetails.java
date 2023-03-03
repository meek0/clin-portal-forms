package bio.ferlab.clin.portal.forms;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Data
public class UserDetails {

  private boolean isSystem = false;
}
