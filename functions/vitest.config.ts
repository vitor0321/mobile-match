import {defineConfig} from "vitest/config";

export default defineConfig({
  test: {
    fileParallelism: false,
    projects: [
      {
        test: {
          name: "unit",
          include: ["test/unit/**/*.test.ts"],
          environment: "node",
        },
      },
      {
        test: {
          name: "rules",
          include: ["test/rules/**/*.test.ts"],
          environment: "node",
        },
      },
    ],
  },
});
