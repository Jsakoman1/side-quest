import {watch, type WatchSource} from "vue"
import {sidequestApi, type Quest} from "../api/sidequestApi.ts"
import {usePaginatedResults} from "./usePaginatedResults.ts"

export const useQuestSearchResults = (
  itemsPerPage: number,
  buildParams: (page: number) => Record<string, string | number | boolean>
) => {
  const results = usePaginatedResults<Quest>(itemsPerPage)

  const loadQuests = async (page = 1) => {
    results.isLoading.value = true

    try {
      const response = await sidequestApi.searchQuests(buildParams(Math.max(0, page - 1)))
      results.applyPage(response)
    } catch {
      results.reset()
    } finally {
      results.isLoading.value = false
    }
  }

  const watchAndReload = (sources: WatchSource[]) => {
    watch(sources, () => {
      void loadQuests(1)
    }, {immediate: true})
  }

  return {
    results,
    loadQuests,
    watchAndReload
  }
}
