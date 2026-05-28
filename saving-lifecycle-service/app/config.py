from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "saving-lifecycle-service"
    port: int = 8088

    # JWT (parse-only)
    jwt_secret: str = "your-super-secret-jwt-key-must-be-at-least-256-bits-long-for-hs256"

    # Downstream service URLs
    contract_service_url: str  = "http://saving-contract-service:8085"
    interest_service_url: str  = "http://saving-interest-service:8087"
    account_service_url: str   = "http://account-service:8083"
    transaction_service_url: str = "http://saving-transaction-service:8086"

    # Scheduler
    maturity_cron_hour: int    = 1   # 01:00 UTC daily — mark matured contracts
    maturity_cron_minute: int  = 0
    interest_cron_hour: int    = 6   # 06:00 UTC daily — pay periodic interest
    interest_cron_minute: int  = 0
    pre_maturity_days: int     = 3   # Notify N days before maturity

    class Config:
        env_file = ".env"


settings = Settings()
