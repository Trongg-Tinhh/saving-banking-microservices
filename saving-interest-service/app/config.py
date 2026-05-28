from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    jwt_secret: str = "your-super-secret-jwt-key-must-be-at-least-256-bits-long-for-hs256"
    app_name: str = "saving-interest-service"
    port: int = 8087

    class Config:
        env_file = ".env"


settings = Settings()
