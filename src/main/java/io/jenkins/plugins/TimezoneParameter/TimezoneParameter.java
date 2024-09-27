package hudson.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 * TimezoneParameter class to create a choice parameter with all available time zones and offsets.
 */
public class TimezoneParameter extends SimpleParameterDefinition {

    private static final List<String> AVAILABLE_TIMEZONES = Arrays.asList(TimeZone.getAvailableIDs());
    private final String defaultValue;
    private boolean calculateOffset;

    @DataBoundConstructor
    public TimezoneParameter(@NonNull String name, @CheckForNull String description) {
        super(name, description);
        this.defaultValue = AVAILABLE_TIMEZONES.get(0);
        this.calculateOffset = true;
    }

    @DataBoundSetter
    public void setCalculateOffset(boolean calculateOffset) {
        this.calculateOffset = calculateOffset;
    }

    public boolean isCalculateOffset() {
        return calculateOffset;
    }

    @NonNull
    @Exported
    public List<String> getChoices() {
        if (calculateOffset) {
            return AVAILABLE_TIMEZONES.stream()
                    .map(tz -> {
                        TimeZone timeZone = TimeZone.getTimeZone(tz);
                        int rawOffsetInSeconds = timeZone.getRawOffset() / 1000;
        
                        int hours = (int) TimeUnit.SECONDS.toHours(rawOffsetInSeconds);
                        int minutes = (int) TimeUnit.SECONDS.toMinutes(rawOffsetInSeconds) % 60;
        
                        if (timeZone.useDaylightTime()) {
                            int dstOffsetInSeconds = timeZone.getDSTSavings() / 1000;
                            hours += (int) TimeUnit.SECONDS.toHours(dstOffsetInSeconds);
                            minutes += (int) TimeUnit.SECONDS.toMinutes(dstOffsetInSeconds) % 60;
                        }
        
                        String offset = String.format("UTC%+03d:%02d", hours, Math.abs(minutes));
                        return tz + " (" + offset + ")";
                    })
                    .collect(Collectors.toList());
        } else {
            return AVAILABLE_TIMEZONES;
        }
    }

    @Override
    @CheckForNull
    public StringParameterValue getDefaultParameterValue() {
        return new StringParameterValue(getName(), defaultValue, getDescription());
    }

    @Override
    public boolean isValid(ParameterValue value) {
        return AVAILABLE_TIMEZONES.contains(((StringParameterValue) value).getValue());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        
        StringParameterValue value = req.bindJSON(StringParameterValue.class, jo);

        String selectedValue = value.getValue();

        if (selectedValue.contains(" (UTC")) {
            selectedValue = selectedValue.substring(0, selectedValue.indexOf(" (UTC"));
        }

        value = new StringParameterValue(getName(), selectedValue, getDescription());
        
        checkValue(value, selectedValue);
        return value;
    }

    private void checkValue(StringParameterValue value, String value2) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Illegal choice for parameter " + getName() + ": " + value2);
        }
    }

    @Override
    public StringParameterValue createValue(String value) {
        StringParameterValue parameterValue = new StringParameterValue(getName(), value, getDescription());
        checkValue(parameterValue, value);
        return parameterValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDescription(), defaultValue, calculateOffset);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        TimezoneParameter other = (TimezoneParameter) obj;
        return Objects.equals(getName(), other.getName()) &&
               Objects.equals(getDescription(), other.getDescription()) &&
               Objects.equals(defaultValue, other.defaultValue) &&
               calculateOffset == other.calculateOffset;
    }

    @Extension @Symbol({"timezone", "timezoneParam"})
    public static class DescriptorImpl extends ParameterDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Timezone Choice Parameter";
        }
        
        public ListBoxModel doFillSelectedTimezoneItems() {
            ListBoxModel items = new ListBoxModel();
            for (String id : TimeZone.getAvailableIDs()) {
                items.add(id, id);
            }
            return items;
        }

        @Override
        public ParameterDefinition newInstance(@CheckForNull StaplerRequest req, @NonNull JSONObject formData) throws FormException {
            String name = formData.getString("name");
            String desc = formData.getString("description");
            boolean calculateOffset = formData.getBoolean("calculateOffset");
            TimezoneParameter parameter = new TimezoneParameter(name, desc);
            parameter.setCalculateOffset(calculateOffset);
            return parameter;
        }
    }
}
