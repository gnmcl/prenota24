-- Le email degli utenti ora si salvano in minuscolo e senza spazi, e si cercano senza
-- distinzione di maiuscole. Qui si normalizzano quelle esistenti.
--
-- Si aggiornano solo le righe che non creano collisioni: se esistessero due account che
-- differiscono solo per le maiuscole, restano invariati e vanno sistemati a mano.
UPDATE app_user u
SET email = lower(trim(u.email))
WHERE u.email <> lower(trim(u.email))
  AND NOT EXISTS (
      SELECT 1 FROM app_user other
      WHERE other.id <> u.id
        AND lower(trim(other.email)) = lower(trim(u.email))
  );

-- Indice univoco case-insensitive, creato solo se non ci sono doppioni da sistemare
-- (altrimenti la migrazione fallirebbe e bloccherebbe l'avvio).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT lower(email) FROM app_user GROUP BY lower(email) HAVING count(*) > 1
    ) THEN
        CREATE UNIQUE INDEX uq_app_user_email_lower ON app_user (lower(email));
    ELSE
        RAISE WARNING 'app_user contiene email duplicate (maiuscole/minuscole): indice uq_app_user_email_lower non creato';
    END IF;
END
$$;
