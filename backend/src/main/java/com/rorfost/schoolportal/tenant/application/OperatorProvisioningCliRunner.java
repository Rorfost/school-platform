package com.rorfost.schoolportal.tenant.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Operator CLI runner for executing safe provisioning commands in an approved environment. Active
 * ONLY under profile 'provisioning' or 'operator-cli'.
 *
 * <p>Usage example:
 *
 * <pre>
 * java -Dspring.profiles.active=provisioning -jar backend.jar \
 *   --provision.slug=school-b \
 *   --provision.name="School B Secondary" \
 *   --provision.hostname=school-b.workers.dev \
 *   --provision.principalEmail=principal@school-b.org \
 *   --provision.initialPassword=ApprovedSecretPass123!
 * </pre>
 */
@Component
@Profile({"provisioning", "operator-cli"})
public class OperatorProvisioningCliRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperatorProvisioningCliRunner.class);

    private final TenantProvisioningService provisioningService;

    public OperatorProvisioningCliRunner(TenantProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Override
    public void run(ApplicationArguments args) {
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
                        : null;

        log.info("Starting operator provisioning for school slug={}", slug);

        ProvisioningResult result =
                provisioningService.provisionSchool(
                        slug, name, hostname, principalEmail, initialPassword);

        // Print directly to standard output once for the operator. Never use logger for the raw
        // token.
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
        System.out.println("GATEWAY TOKEN (DISPLAYED ONCE - STORE SECURELY IN CLOUDFLARE):");
        System.out.println(result.rawGatewayToken());
        System.out.println("=================================================================");
    }

    private String getRequiredOption(ApplicationArguments args, String name) {
        if (!args.containsOption(name) || args.getOptionValues(name).isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required provisioning parameter: --" + name);
        }
        return args.getOptionValues(name).get(0);
    }
}
