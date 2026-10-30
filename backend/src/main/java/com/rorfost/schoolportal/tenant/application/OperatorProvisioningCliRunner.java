package com.rorfost.schoolportal.tenant.application;

import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Operator CLI runner for executing safe provisioning and feature management commands in an
 * approved environment. Active ONLY under profile 'provisioning' or 'operator-cli'.
 *
 * <p>Usage examples:
 *
 * <pre>
 * # School Provisioning:
 * java -Dspring.profiles.active=provisioning -jar backend.jar \
 *   --provision.slug=school-b \
 *   --provision.name="School B Secondary" \
 *   --provision.hostname=school-b.workers.dev \
 *   --provision.principalEmail=principal@school-b.org
 *
 * # Feature Flag Management:
 * java -Dspring.profiles.active=operator-cli -jar backend.jar \
 *   --feature.slug=school-b \
 *   --feature.key=GALLERY \
 *   --feature.enabled=true
 * </pre>
 */
@Component
@Profile({"provisioning", "operator-cli"})
public class OperatorProvisioningCliRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperatorProvisioningCliRunner.class);

    private final TenantProvisioningService provisioningService;
    private final FeatureFlagService featureFlagService;

    public OperatorProvisioningCliRunner(
            TenantProvisioningService provisioningService, FeatureFlagService featureFlagService) {
        this.provisioningService = provisioningService;
        this.featureFlagService = featureFlagService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("feature.slug")) {
            runFeatureFlagCommand(args);
            return;
        }

        if (!args.containsOption("provision.slug")) {
            return;
        }

        String slug = getRequiredOption(args, "provision.slug");
        String name = getRequiredOption(args, "provision.name");
        String hostname = getRequiredOption(args, "provision.hostname");
        String principalEmail = getRequiredOption(args, "provision.principalEmail");
        String initialPassword =
                args.containsOption("provision.initialPassword")
                        ? args.getOptionValues("provision.initialPassword").get(0)
                        : System.getenv("ADMIN_INITIAL_PASSWORD");

        if (initialPassword == null || initialPassword.isBlank()) {
            initialPassword = System.getProperty("provision.initialPassword");
        }

        log.info("Starting operator provisioning for school slug={}", slug);

        ProvisioningResult result =
                provisioningService.provisionSchool(
                        slug, name, hostname, principalEmail, initialPassword);

        // Print directly to standard output once for the operator. Never use logger for the raw
        // token, and never log or print the initial password.
        System.out.println("=================================================================");
        System.out.println("OPERATOR PROVISIONING COMPLETED SUCCESSFULLY");
        System.out.println("School UUID        : " + result.schoolId());
        System.out.println("School Slug        : " + result.slug());
        System.out.println("School Name        : " + result.name());
        System.out.println("Mapped Hostname    : " + result.hostname());
        System.out.println("Principal Email    : " + result.principalEmail());
        System.out.println("Credential Label   : " + result.tokenLabel());
        System.out.println("Credential UUID    : " + result.credentialId());
        System.out.println("-----------------------------------------------------------------");
        if (result.rawGatewayToken() != null) {
            System.out.println("GATEWAY TOKEN (DISPLAYED ONCE - STORE SECURELY IN CLOUDFLARE):");
            System.out.println(result.rawGatewayToken());
        } else {
            System.out.println("GATEWAY TOKEN      : [RETAINED EXISTING ACTIVE CREDENTIAL]");
        }
        System.out.println("=================================================================");
    }

    private void runFeatureFlagCommand(ApplicationArguments args) {
        String slug = getRequiredOption(args, "feature.slug");
        String key = getRequiredOption(args, "feature.key");
        String enabledStr = getRequiredOption(args, "feature.enabled");
        boolean enabled = Boolean.parseBoolean(enabledStr.strip());

        log.info("Setting feature flag: school={}, key={}, enabled={}", slug, key, enabled);
        featureFlagService.setSchoolFeatureBySlug(slug, key, enabled);

        System.out.println("=================================================================");
        System.out.println("OPERATOR FEATURE FLAG UPDATED SUCCESSFULLY");
        System.out.println("School Slug        : " + slug);
        System.out.println("Feature Key        : " + key.toUpperCase(java.util.Locale.ROOT));
        System.out.println("Enabled            : " + enabled);
        System.out.println("=================================================================");
    }

    private String getRequiredOption(ApplicationArguments args, String name) {
        if (!args.containsOption(name) || args.getOptionValues(name).isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: --" + name);
        }
        return args.getOptionValues(name).get(0);
    }
}
