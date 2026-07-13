terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# LocalStack provider — points all API calls to the local emulator.
# Day 18 replaces this block with a real AWS provider and S3 remote state backend.
# For local runs, set env. vars. AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY as "test"
provider "aws" {
  region                      = var.region
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    dynamodb = var.local_endpoint
    s3       = var.local_endpoint
    iam      = var.local_endpoint
    ecr      = var.local_endpoint
    ecs      = var.local_endpoint
    sts      = var.local_endpoint
  }
}
