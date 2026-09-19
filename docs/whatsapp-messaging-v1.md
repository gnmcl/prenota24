# WhatsApp messaging V1 setup

Messaging is disabled by default. Apply Flyway migration `V15__messaging_v1.sql`, then configure exactly one pilot studio and one WhatsApp Business destination per deployment.

Required environment variables when enabling it:

```text
WHATSAPP_ENABLED=true
WHATSAPP_PILOT_STUDIO_ID=<studio UUID>
WHATSAPP_PHONE_NUMBER_ID=<Meta phone number id>
WHATSAPP_ACCESS_TOKEN=<system-user access token>
WHATSAPP_APP_SECRET=<Meta app secret>
WHATSAPP_VERIFY_TOKEN=<random webhook verification token>
WHATSAPP_TEMPLATE_NAME=<approved parameterless template name>
WHATSAPP_TEMPLATE_LANGUAGE=it
WHATSAPP_API_VERSION=v25.0
```

Optional timeout and polling settings are `WHATSAPP_CONNECT_TIMEOUT` (default `5s`), `WHATSAPP_REQUEST_TIMEOUT` (default `10s`) and `WHATSAPP_DISPATCH_DELAY_MS` (default `2000`). Do not place secrets in committed configuration.

Register the callback URL `https://<host>/api/webhooks/whatsapp` in Meta and subscribe to `messages`. Meta's GET challenge uses `WHATSAPP_VERIFY_TOKEN`; POST requests are accepted only when `X-Hub-Signature-256` matches the HMAC-SHA256 of the exact raw request body using `WHATSAPP_APP_SECRET`.

The app must also be subscribed to the WhatsApp Business Account (`/{WABA-ID}/subscribed_apps`), and its token must have the relevant WhatsApp messaging permissions. See [Meta's official Cloud API collection](https://www.postman.com/meta/whatsapp-business-platform/documentation/wlk6lh4/whatsapp-cloud-api). Verify the configured Graph API version in the Meta dashboard when activating the integration.

Free text is allowed only with recorded opt-in and an inbound message in the preceding 24 hours. The same checks run when the send is queued and immediately before dispatch. The configured template is parameterless; arbitrary template text is rejected. A transport timeout after dispatch becomes `UNKNOWN` and is not automatically retried because delivery may already have occurred.

Inbound senders are linked only when their normalized number matches exactly one client in the pilot studio. Missing, shared, or wrong-destination numbers are stored in `whatsapp_inbound_quarantine` for manual review and are never attached to client history automatically.

Client phone numbers must be stored in explicit international format, for example `+39 333 1234567` (or `0039...`). National numbers such as `3331234567` are blocked; V1 never guesses a country code. Consent and the inbound 24-hour window are bound to the normalized recipient number, so editing a client's phone requires recording consent again and receiving a new inbound message before free text can be sent.

## User workflow and V1 limits

An ADMIN opens **Apri conversazione** from appointment details, or uses **Messaggi** in the sidebar. There is one shared conversation per client in the studio, independent of the number of appointments. Each administrator has a separate read cursor. The optional appointment reference on an outgoing message remains visible in history.

The operator records consent already obtained from the client; checking the box does not collect consent from the patient. The inbox refreshes every 10 seconds while visible. QUEUED means stored locally, ACCEPTED means accepted by Meta, and DELIVERED/READ come from signed status callbacks. UNKNOWN requires manual verification; there is no automatic resend.

V1 supports one configured studio/number per deployment, ADMIN access, text messages and one parameterless approved template. It does not include attachments, professional access, assignment, patient accounts, multi-number onboarding, or a screen for resolving quarantined messages. Unsupported inbound media is represented by a placeholder. The template history shows its configured name, not a fetched copy of the approved template body. Quarantined payloads require restricted operational review; retention and resolution automation are not included.

## Local verification

Use Java 25. `./mvnw test` runs the unit suite; PostgreSQL integration tests are skipped unless `MESSAGING_DB_TEST=true` is set. To run all tests, provision an empty, disposable PostgreSQL database whose name starts with `messaging_v1_`, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `RESEND_API_KEY` and `RESEND_FROM_ADDRESS` to local test values, then run:

```sh
MESSAGING_DB_TEST=true ./mvnw test
```

The integration test uses profile `test`, applies Flyway migrations and validates Hibernate mappings. It seeds and deletes its own fixture studio, exercises real HTTP on a random port, and replaces WhatsApp dispatch/gateway beans with mocks so no messages can leave the test process. Never point it at a shared or production database.

Verified locally: 20 backend tests including five PostgreSQL/HTTP integration tests, 12 frontend behavior tests and Angular production build. Desktop/mobile UI checks used synthetic fixtures. Real Meta delivery has not been exercised; after deployment/configuration, validate challenge, inbound reply, template delivery and delivery/read callbacks using an authorized test recipient before opening the pilot.
