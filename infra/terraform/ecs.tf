# ECS cluster, task definition (Fargate, distroless Java 21), and service

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.project}"
  retention_in_days = 7

  tags = local.common_tags
}

resource "aws_ecs_cluster" "testgen" {
  name = "${var.project}-ecs-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "testgen" {
  family                   = "${var.project}-family"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.testgen_ecs_execution_role.arn
  task_role_arn            = aws_iam_role.testgen_ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = local.container_name
      image     = "${aws_ecr_repository.app.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "AWS_REGION", value = var.region },
        { name = "AWS_S3_BUCKET", value = aws_s3_bucket.artifacts.bucket },
        # TODO(Day 20): remove once /testgen/llm-api-key is wired via SSM and the real provider can start
        { name = "TESTGEN_LLM_PROVIDER", value = "noop" },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "ai-test-gen-engine"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "testgen" {
  name            = "${var.project}-ecs-service"
  cluster         = aws_ecs_cluster.testgen.id
  task_definition = aws_ecs_task_definition.testgen.arn
  launch_type     = "FARGATE"
  desired_count   = 1

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.testgen.arn
    container_name   = local.container_name
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.testgen]

  tags = local.common_tags
}
