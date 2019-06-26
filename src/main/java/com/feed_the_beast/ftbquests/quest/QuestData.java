package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftbquests.quest.reward.QuestReward;
import com.feed_the_beast.ftbquests.quest.task.QuestTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public abstract class QuestData
{
	public final Int2ObjectOpenHashMap<QuestTaskData> taskData;
	public final Map<UUID, IntOpenHashSet> claimedPlayerRewards;
	public final IntOpenHashSet claimedTeamRewards;

	protected QuestData()
	{
		taskData = new Int2ObjectOpenHashMap<>();
		claimedPlayerRewards = new HashMap<>();
		claimedTeamRewards = new IntOpenHashSet();
	}

	public abstract short getTeamUID();

	public abstract String getTeamID();

	public abstract ITextComponent getDisplayName();

	public abstract QuestFile getFile();

	public QuestTaskData getQuestTaskData(QuestTask task)
	{
		QuestTaskData data = taskData.get(task.id);

		if (data == null)
		{
			return task.createData(this);
		}

		return data;
	}

	public void syncTask(QuestTaskData data)
	{
		getFile().clearCachedProgress(getTeamUID());
	}

	public void removeTask(QuestTask task)
	{
		taskData.remove(task.id);
	}

	public void createTaskData(QuestTask task)
	{
		QuestTaskData data = task.createData(this);
		taskData.put(task.id, data);
		data.isComplete = data.getProgress() >= data.task.getMaxProgress();
	}

	public boolean isRewardClaimed(UUID player, QuestReward reward)
	{
		if (reward.isTeamReward())
		{
			return claimedTeamRewards.contains(reward.id);
		}

		IntOpenHashSet rewards = claimedPlayerRewards.get(player);
		return rewards != null && rewards.contains(reward.id);
	}

	public void unclaimRewards(Collection<QuestReward> rewards)
	{
		for (QuestReward reward : rewards)
		{
			if (reward.isTeamReward())
			{
				claimedTeamRewards.rem(reward.id);
			}
			else
			{
				Iterator<IntOpenHashSet> iterator = claimedPlayerRewards.values().iterator();

				while (iterator.hasNext())
				{
					IntOpenHashSet set = iterator.next();

					if (set != null && set.rem(reward.id))
					{
						if (set.isEmpty())
						{
							iterator.remove();
						}
					}
				}
			}
		}
	}

	public boolean setRewardClaimed(UUID player, QuestReward reward)
	{
		if (reward.isTeamReward())
		{
			if (claimedTeamRewards.add(reward.id))
			{
				reward.quest.checkRepeatableQuests(this, player);
				return true;
			}
		}
		else
		{
			IntOpenHashSet set = claimedPlayerRewards.get(player);

			if (set == null)
			{
				set = new IntOpenHashSet();
			}

			if (set.add(reward.id))
			{
				if (set.size() == 1)
				{
					claimedPlayerRewards.put(player, set);
				}

				reward.quest.checkRepeatableQuests(this, player);
				return true;
			}
		}

		return false;
	}

	public void checkAutoCompletion(Quest quest)
	{
	}
}